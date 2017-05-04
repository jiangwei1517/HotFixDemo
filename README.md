# HotFixDemo
![MacDown logo](https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1493910118836&di=9529e7a93c0e5838b9f4aff324e3ab9d&imgtype=0&src=http%3A%2F%2Fs14.sinaimg.cn%2Fmw690%2F002KDSPGgy6InPL0KqF7d%26690)

## 参考资料

* Android热补丁动态修复技术（一）：从Dex分包原理到热补丁<http://blog.csdn.net/u010386612/article/details/50885320>
* Android热补丁动态修复技术（二）：实战！CLASS_ISPREVERIFIED问题！<http://blog.csdn.net/u010386612/article/details/51077291>
* Android热补丁动态修复技术（三）—— 使用Javassist注入字节码，完成热补丁框架雏形<http://blog.csdn.net/u010386612/article/details/51131642>
* Android热补丁动态修复技术（四）：自动化生成补丁——解决混淆问题<http://blog.csdn.net/u010386612/article/details/51192421>

## 原理
如果两个dex中存在相同的class文件会怎样？ 
先从第一个dex中找，找到了直接返回，遍历结束。而第二个dex中的class永远不会被加载进来。 
简而言之，两个dex中存在相同class的情况下，dex1的class会覆盖dex2的class。

而热补丁技术则利用了这一特性，当一个app出现bug的时候，我们就可以将出现那个bug的类修复后，重新编译打包成dex，插入到dexElements的前面，那么出现bug的类就会被覆盖，app正常运行，这就是热修复的原理了。 

## 以Person为例

* 修改Person类的名字，生成的class文件打包成jar（注意包名要相同）
		
		jar -vcf patch.jar com
* 将jar包打包成二进制dex文件

		dx --dex --output=patch_dex.jar patch.jar
	
* 将patch_dex.jar放入sd卡，（实际不建议这么做）

		public static void findDex(String patchName, Context context) {
	        String dexPath =
	                Environment.getExternalStorageDirectory().getAbsolutePath().concat(File.separator + patchName);
	        File file = new File(dexPath);
	        if (file.exists()) {
	            ReflectUtils.inject(dexPath, context);
	            Log.e("BugFixApplication", dexPath + "存在");
	        } else {
	            Log.e("BugFixApplication", dexPath + "不存在");
	        }
	    }
	   
* 分别加载两个不同的dex文件，将补丁的dex文件优先加载

		 Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
	                Field pathListField = cl.getDeclaredField("pathList");
	                pathListField.setAccessible(true);
	                pathListField.get(context.getClassLoader());
	                Object pathList = getField(cl, "pathList", context.getClassLoader());
	                Object baseElements = getField(pathList.getClass(), "dexElements", pathList);
	
	                // 获取patch_dex的dexElements（需要先加载dex）
	                String dexopt = context.getDir("dexopt", 0).getAbsolutePath();
	                // optimizedDirector 优化后的dex文件存放目录，不能为null
	                // libraryPath 目标类中使用的C/C++库的列表,每个目录用File.pathSeparator间隔开; 可以为 null
	                // parent 该类装载器的父装载器，一般用当前执行类的装载器
	                DexClassLoader dexClassLoader = new DexClassLoader(path, dexopt, dexopt, context.getClassLoader());
	                Object obj = getField(cl, "pathList", dexClassLoader);
	                Object dexElements = getField(obj.getClass(), "dexElements", obj);
	
	                // 合并两个Elements
	                Object combineElements = combineArray(dexElements, baseElements);
	                
### 问题--->CLASS_ISPREVERIFIED
	                
我在这里总结了一个过程，想知道详细分析过程的请看QQ空间开发团队的原文。
在apk安装的时候，虚拟机会将dex优化成odex后才拿去执行。在这个过程中会对所有class一个校验。
校验方式：假设A该类在它的static方法，private方法，构造函数，override方法中直接引用到B类。如果A类和B类在同一个dex中，那么A类就会被打上CLASS_ISPREVERIFIED标记
被打上这个标记的类不能引用其他dex中的类，否则就会报图中的错误
在我们的Demo中，MainActivity和Person本身是在同一个dex中的，所以MainActivity被打上了CLASS_ISPREVERIFIED。而我们修复bug的时候却引用了另外一个dex的Person.class，所以这里就报错了
而普通分包方案则不会出现这个错误，因为引用和被引用的两个类一开始就不在同一个dex中，所以校验的时候并不会被打上CLASS_ISPREVERIFIED
### 解决方法
补充一下第二条：A类如果还引用了一个C类，而C类在其他dex中，那么A类并不会被打上标记。换句话说，只要在static方法，构造方法，private方法，override方法中直接引用了其他dex中的类，那么这个类就不会被打上CLASS_ISPREVERIFIED标记。

在所有类的构造函数中插入这行代码 System.out.println(RemoteTagDex.class); 
这样当安装apk的时候，classes.dex内的类都会引用一个在不相同dex中的RemoteTagDex.class类，这样就防止了类被打上CLASS_ISPREVERIFIED的标志了，只要没被打上这个标志的类都可以进行打补丁操作。
tag_dex.dex在应用启动的时候就要先加载出来，不然RemoteTagDex.class类会被标记为不存在，即使后面再加载tag_dex.dex，RemoteTagDex.class类还是会提示不存在。该类只要一次找不到，那么就会永远被标上找不到的标记了。
我们一般在Application中执行dex的注入操作，所以在Application的构造中不能加上System.out.println(RemoteTagDex.class);这行代码，因为此时tag_dex.dex还没有加载进来，RemoteTagDex.class并不存在。
之所以选择构造函数是因为他不增加方法数，一个类即使没有显式的构造函数，也会有一个隐式的默认构造函数。

## 引入javaassist注入二进制代码
		
	package com.jiangwei.plugin
		
	import com.android.build.api.transform.*
	import com.android.build.gradle.internal.pipeline.TransformManager
	import org.apache.commons.codec.digest.DigestUtils	import org.apache.commons.io.FileUtils
	import org.gradle.api.Project
			
	public class PreDexTransform extends Transform {
	
	    Project project
	    // 添加构造，为了方便从plugin中拿到project对象，待会有用
	    public PreDexTransform(Project project) {
	        this.project = project
	        def libPath = project.project(':tagdex').buildDir.absolutePath.concat(File.separator + "intermediates" + File.separator + "classes" + File.separator + "debug")
	        Inject.appendClassPath(libPath)
	        // 解决Context报错
	        Inject.appendClassPath("/Users/baidu/Library/Android/sdk/platforms/android-24/android.jar")
	    }
	
	    // Transfrom在Task列表中的名字
	    // TransfromClassesWithPreDexForXXXX
	    @Override
	    String getName() {
	        return "preDex";
	    }
	
	    @Override
	    Set<QualifiedContent.ContentType> getInputTypes() {
	        return TransformManager.CONTENT_CLASS;
	    }
	
	    @Override
	    Set<QualifiedContent.Scope> getScopes() {
	        return TransformManager.SCOPE_FULL_PROJECT;
	    }
	
	    @Override
	    boolean isIncremental() {
	        return false
	    }
	
	    @Override
	    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
	        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
	
	        // Transfrom的inputs有两种类型，一种是目录，一种是jar包，要分开遍历
	        inputs.each { TransformInput input ->
	
	            input.directoryInputs.each { DirectoryInput directoryInput ->
	
	                // 获取output目录
	                def dest = outputProvider.getContentLocation(directoryInput.name,
	                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
	
	
	                // 这里可以对input的文件做处理，比如代码注入！
	                Inject.injectDir(directoryInput.file.absolutePath)
	
	
	                // 将input的目录复制到output指定目录
	                FileUtils.copyDirectory(directoryInput.file, dest)
	            }
	
	            input.jarInputs.each { JarInput jarInput ->
	
	                // 重命名输出文件（同目录copyFile会冲突）
	                def jarName = jarInput.name
	                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
	                if (jarName.endsWith(".jar")) {
	                    jarName = jarName.substring(0, jarName.length() - 4)
	                }
	                def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
	
	
	                // 这里可以对input的文件做处理，比如代码注入！
	                String jarPath = jarInput.file.absolutePath;
	                String projectName = project.rootProject.name;
	                if (jarPath.endsWith("classes.jar")
	                        && jarPath.contains("exploded-aar" + File.separator + projectName)
	                        // hotutils module是用来加载dex，无需注入代码
	                        && !jarPath.contains("exploded-aar" + File.separator + projectName + File.separator + "hotutils")) {
	                    Inject.injectJar(jarPath)
	                }
	
	                // 将input的目录复制到output指定目录
	                FileUtils.copyFile(jarInput.file, dest)
	            }
	        }
	    }
	}
	


 


