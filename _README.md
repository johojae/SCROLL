# SCROLL
KAIST TEAM PROJECT


## 개발환경 구축
### OpenCV Android Project 적용방법
OpenCV SDK를 다운로드하고 프로젝트 적용을 위한 설정을 추가한다

* OpenCV SDK for Android Download(https://opencv.org/releases/)
  * https://github.com/opencv/opencv/releases/download/4.8.0/opencv-4.8.0-android-sdk.zip

* 다운로드한 파일을 압축해제하여 sdk 폴더만 안드로이드 프로젝트 폴더로 옮겨서 따로 보관
  
* opencv 프로젝트 파일 수정
  * build.gradle의 android 설정에 아래 항목들 추가
    <pre>
      <code>
      android {
      	namespace "org.opencv"
      	buildFeatures {
      		aidl true
      		buildConfig true
      	}
      	java {
      		toolchain {
      			languageVersion = JavaLanguageVersion.of(17)
      		}
      	}
      }
      </code>
    </pre>

  * java버전 관련 오류가 있을경우 아래와 같이 변경이 필요할 수 있음
    <pre>
      <code>
    android {
    	compileOptions {
    		sourceCompatibility JavaVersion.VERSION_17
    		targetCompatibility JavaVersion.VERSION_17
    	}
    }
      </code>
    </pre>

* opencv의 AndroidManifest.xml에서 namespace 제거
  * \<manifest xmlns:android="http://schemas.android.com/apk/res/android" namespace="org.opencv.android"/\> <- 여기서 namespace를 지워야 한다
    
### 안드로이드 프로젝트에 OpenCV 적용
생성한 안드로이드 프로젝트에 opencv를 모듈로 추가하고 dependency를 적용한다
  
* settings.gradle
  * projectDir은 다운로드하여 받은 폴더 중 sdk 폴더만 따로 옮긴 경로
  <pre>
    <code>
      include(":opencv")
      project(":opencv").projectDir = File("/home/scroll/AndroidStudioProjects/opencv-sdk-4.8.0")
    </code>
  </pre>

* OpenCV 모듈 등록을 위해 안드로이드 스튜디오의 메뉴를 아래와 같이 선택
  * File → ‘Sync Project with Gradle Files’
  * File → Project Structure → Dependencies → add module dependency ‘opencv’ on ’app’ module

* reference
  * add opencv library: https://stackoverflow.com/questions/76831677/trying-to-import-opencv-4-8-in-android-studio-giraffe
  * https://proandroiddev.com/android-studio-step-by-step-guide-to-download-and-install-opencv-for-android-9ddcb78a8bc3
