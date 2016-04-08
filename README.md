# BuzzStore Integration Guide v0.8

버즈스토어를 연동하기 위한 통합 가이드.

버즈스토어를 모바일에서 띄우기 위해서는 크게 SDK 연동과 API 구현이 필요하다.

버즈스토어는 보안상으로 안전하게 이용하기 위해서 앱아이디(APP_ID), 앱토큰(APP_TOKEN), 퍼블리셔 유저아이디(USER_ID), 퍼블리셔 유저토큰(USER_TOKEN)을 필수 파라미터로 사용하고 있다. 
- APP_ID는 버즈스토어에서 퍼블리셔 앱을 구별하기 위한 고유 식별자로, 연동 전에 지급하며 어드민에서 확인할 수 있다. 
- APP_TOKEN은 앱만의 고유한 값으로 아래의 4번 절차를 통해서 얻을 수 있다. 
- USER_ID 는 퍼블리셔에서 관리하는 유저의 고유 식별자이다. 
- USER_TOKEN 은 일종의 유저 인증번호이다. 
이 중 APP_ID, USER_ID는 기존에 정해져 있는 값이며, APP_TOKEN은 key signing 과 관련된 값으로 별도의 통신이 필요 없다. USER_TOKEN을 얻기 위해서는 SDK에서 제공해주는 UserToken 유효성 체크 인터페이스의 구현, 퍼블리셔 측 API 구현 및 Server-To-Server 연동이 필요하다.

## BuzzStore SDK for Android integration
- 버즈스토어를 안드로이드 어플리케이션에 연동하기 위한 라이브러리
- 안드로이드 버전 지원 : Android 4.0.3(API Level 15) 이상
- 스토어 목록은 웹뷰로 이루어져 있으며, SDK를 이용해 필수 파라미터를 전달하여 모바일 UI를 호출한다.
- 보안을 위한 user Token 관리를 위해 API call 을 구현해야 한다.
- SDK 연동은 크게 UserToken 유효성 체크부분과 스토어 UI 사용부분으로 나뉘어져 있다.

#### UserToken 유효성 체크 인터페이스
- `BuzzStore.setBuzzStoreListener(BuzzStoreListener listener)` : BuzzStoreListener interface를 구현할 수 있다. 
- BuzzStoreListenr interface 구조
    - `String needCall()` : userToken이 invalidate 할 때 호출된다. needCall이 호출되면 아래에 명시된 Interface를 가지는 publisher API를 호출해야 한다. API호출을 통해 전달받은 해당 유저의 userToken을 리턴한다.
    - `void onFail()` : 몇 차례 userToken validation을 시도했으나 최종 실패한 경우 호출된다. 실패 시 UI처리를 위해 사용할 수 있다.

```Java
BuzzStore.setBuzzStoreListener(new BuzzStore.BuzzStoreListener() {
    @Override
    public String needCall() {
        // 해당 함수가 호출 되면 해당 유저의 유저토큰이 리턴되어야 한다.
        // 보안상의 이유로 해당 유저토큰은 서버 투 서버로만 제공되므로 퍼블리셔앱은 반드시
        // 퍼블리셔 서버를 통해서 버즈스토어로 요청해야 한다. 자세한 부분은 2번 사항 참조.
        return "EXAMPLE_USER_TOKEN";
    }

    @Override
    public void onFail() {
        // 최대 횟수 만큼 SDK 내부에서 유저토큰 벨리데이션이 실패한 경우 이 함수가 호출 된다.
        // 이 때, 퍼블리셔는 앱단에서 실패 UI 등을 정의해서 처리해야 한다.
        Toast.makeText(this, "Fail to Load BuzzStore", Toast.LENGTH_LONG).show();
    }
});
```

#### 버즈스토어 모바일 UI 호출
- `BuzzStore.init(String appId, String userId, Context context)` : 초기화 함수로, 버즈스토어를 로드하려는 액티비티의 onCreate 에 호출한다.

- `BuzzStore.loadStore(Activity activity)` : 버즈스토어 모바일 UI를 호출한다.
> **주의** : 반드시 `init()`이 먼저 호출된 이후에 호출해야 한다.

#### 사용 예제
```Java
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        /**
         * Initialize BuzzStore.
         * BuzzStore.init have to be called prior to BuzzStore.loadStore
         * appId : Unique key value to identify the publisher.
         * userId : user identifier used from publisher
         * this : Context
         */
        BuzzStore.init("appId", "userId", this);

        findViewById(R.id.showStoreButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * Load BuzzStore.
                 * MainActivity.this : Current activity
                 */
                BuzzStore.loadStore(MainActivity.this);
            }
        });
    }
}
```

#### 기본 설정

- [SDK 다운로드](https://github.com/Buzzvil/buzzstore-sdk-publisher/archive/master.zip) 후 압축해제하여 buzzstore-sdk/buzzstore.jar를 개발중인 안드로이드 어플리케이션에 라이브러리로 포함시킨다.
- AndroidManifest.xml 에 다음과 같이 액티비티와 퍼미션을 추가한다.

```Xml
<manifest>
    ...
    <!-- permission for BuzzStore -->
    <uses-permission android:name="android.permission.INTERNET" />

    <!-- activity for BuzzStore -->
    <activity
            android:name="com.buzzvil.buzzstore.StoreActivity"
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />
    </application>
    ...
</manifest>
```

- Proguard 설정
Proguard 사용 시 다음 라인들을 Proguard 설정에 추가한다.
```
-keep class com.buzzvil.buzzstore.** {*;}
-keep interface com.buzzvil.buzzstore.** {*;}
```

## 2. 퍼블리셔 API Interface
위 유효성 체크 인터페이스의 needCall 함수를 구현하기 위해서는 1) 퍼블리셔 앱(PA)에서 퍼블리셔 서버(PS)를 거쳐 2) 버즈스토어 서버(BS)로 유저토큰을 요청해야 한다. 2)번 단계는 항상 1)번 단계에 의해 일어나야 한다. 표면적으로 PS가 PA에게 유저토큰을 전달하는 것처럼 보이지만, PS는 내부적으로 유저 토큰을 저장하지 않고 BS로부터 전달받은 토큰을 재전달하는 중계역할만을 수행한다. 보안상의 이유로 PA는 BS에 직접적으로 유저토큰을 요청 할 수 없다.
- Request 주체 : Publisher App(PA)
- 중계자 : Publisher Server(PS)
- Response 주체 : BuzzStore Server(BS)
- PA 가 PS로 request를 보내면, PS는 BS로 request를 보낸다. 자세한 사항은 3번 참조.
- BS는 userToken 을 생성하여 PS로 보내고, PS는 다시 PA로 보낸다.

SDK의 `BuzzStore.needCall()` 인터페이스 구현 중에 PA와 PS와의 통신을 추가해야 한다.

## 3. BuzzStore server-to-server 연동
이 연동은 2번 퍼블리셔 API Interface 중 PS와 BS간의 통신에 대한 설명이다.

PS는 위 API를 호출하여 BS 로부터 유저의 토큰을 전달받아 PA로 재전달한다. 이 연동을 하기 위해서는 먼저 퍼블리셔 서버의 아이피 주소를 `화이트 리스트`로 등록해야 한다. 화이트 리스트에 등록 될 아이피주소는 별도의 채널(e.g. 이메일)을 통해서 퍼블리셔가 전달한다. 

#### 요청
- API 호출 방향 : 퍼블리셔 서버 -> 버즈스토어 서버
- method : `POST`
- url : `https://52.193.111.153/api/users` (테스트 환경)
- Headers : 다음의 파라미터를 담아서 요청한다.
    - `HTTP-X-BUZZVIL-APP-ID` : 사전에 발급한 퍼블리셔 앱에 부여 된 고유한 아이디.
    - `HTTP-X-BUZZVIL-API-KEY` : 사전에 발급한 서버 투 서버 API 사용을 위한 고유한 API 키
- POST 필수 파라미터 : 퍼블리셔의 유저 식별자 `publisher_user_id`
- e.g.
```JSON
{
"publisher_user_id": 1270537
}
```
- 단, 버즈스토어는 퍼블리셔 유저 식별자를 기준으로 유저를 식별하므로 해당 값은 추후 바꿀 수 없다. 두개 이상의 서로 다른 유저 식별자에 대해서 버즈스토어는 서로 다른 유저로 인식한다. 따라서, 퍼블리셔가 제공하는 유저 식별자는 절대 업데이트되지 않는 값을 권장한다. 예를들어, 추후 변경의 여지가 있는 계정에 연동 된 이메일보다는 퍼블리셔 디비의 고유 식별자로 권장한다.
- 성공 시 JSON 포맷으로 `publisher_user_id`, `token` 를 리턴한다. HTTP 응답 상태 코드는 200 이다. 
- e.g.
```JSON
{
"publisher_user_id": 1270537,
"token": "yYn05pKNlHpMdmBd2GeXU4tBdQtIENuFmFJ0MhBJkwBIzE2TXffLTA7bfWAmRSOc"
}
```
- 실패 시 JSON 포맷으로 `error_code`, `error_message` 를 리턴한다. HTTP 응답 상태 코드는 400 이다.

#### 주의사항 
버즈스토어 서버에 장애가 발생하여 이 API 를 통한 토큰 발급에 실패하는 경우 스토어 SDK 이용이 제한된다. 이 때, 재시도는 SDK 내에서 제공하는 `needCall()` 인터페이스의 호출을 통해 시도되어야 한다.

## 4. Key Hash를 버즈스토어 어드민에 등록
- 퍼블리셔가 버즈스토어 서버에 보내는 request에 대한 보안 강화를 위해 위조가 불가능한 퍼블리셔 식별자가 필요하다. 퍼블리셔 앱의 고유한 Key Hash 값을 이러한 식별자로 이용한다.
- 버즈스토어 어드민에 다음과 같이 콘솔을 이용하거나 코드를 이용해서 얻은 Key Hash를 등록한다.
- Key Hash는 디버그 앱과 릴리즈 앱이 서로 다르다.
- 디버그 앱의 Key Hash는 빌드하는 환경(e.g. 컴퓨터)에 따라 달라진다. 다수의 개발자가 디버그용 앱을 각각 다른 환경에서 빌드할 경우, 각각의 환경에서의 Key Hash를 모두 등록해야 한다.
- 어드민에 Key Hash가 등록되어 있지 않으면 버즈스토어를 로드할 수 없다.

#### (1) 터미널을 이용해서 생성하는 법
##### 디버그용 Key Hash
터미널에서 다음과 같은 command를 통해 디버그용 Key Hash를 얻을 수 있다.
> 주의 : 디버그용 키 저장소의 default 비밀번호는 `android` 이다.

```
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
```

##### 릴리즈용 Key Hash
터미널에서 다음과 같은 command를 통해 릴리즈용 Key Hash를 얻을 수 있다. 아래의 `<Release key alias>` 에는 릴리즈 키의 alias를, `<Release key path>` 에는 릴리즈 키의 path를 입력한다.
> 주의 : 릴리즈용 키를 얻으려면 이전에 지정한 키 저장소 비밀번호 입력이 필요하다.

```
keytool -exportcert -alias <Release key alias> -keystore <Release key path> | openssl sha1 -binary | openssl base64
```

#### (2) 코드를 이용해서 생성하는 법
- 제공하는 sdk 내의 `BuzzStore.getKeyHash(Context context)` 함수를 이용한다. 이 함수는 앱의 context 를 파라미터로 받아 현재 빌드된 앱의 Key Hash를 리턴한다.

