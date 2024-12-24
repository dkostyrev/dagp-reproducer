[Dependency Analysis Gradle Plugin](https://github.com/autonomousapps/dependency-analysis-gradle-plugin) ignores output of any variant transform task, which results in empty ABI report and false positives with advice to replace `api` with `implementation`.

Steps to reproduce:

1. Generate report:
```
 ./gradlew :module:abiAnalysisRelease
```

2. Read result:
```
cat module/build/reports/dependency-analysis/releaseMain/abi-dump.txt
```

The result is empty.

3. Generate report without transform task:
```
./gradlew :module:abiAnalysisRelease -PdoNotTransform
```

4. Read result:
```
cat module/build/reports/dependency-analysis/releaseMain/abi-dump.txt
```

The result is not empty:
```
@Lkotlin/Metadata;
public final class com/dkostyrev/module/SampleClass {
	public fun <init> ()V
	public final fun sampleMethod ()Ljava/lang/String;
}
```
