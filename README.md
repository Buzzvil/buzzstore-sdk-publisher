# BuzzStore Integration Guide v0.8

버즈스토어를 연동하기 위한 통합 가이드.
총 4단계의 연동 작업이 필요하다.

1. 퍼블리셔 앱의 Key Hash를 버즈스토어 어드민에 등록
2. 유저의 보안용 토큰 생성을 위한 버즈스토어 서버와 퍼블리셔 서버 사이의 server-to-server 연동
3. 퍼블리셔 앱 클라이언트로부터 퍼블리셔 서버로의 API call 구현
4. 안드로이드 앱 내에 모바일 UI를 띄우기 위한 SDK 연동

## 1. Key Hash를 버즈스토어 어드민에 등록

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
터미널에서 다음과 같은 command를 통해 릴리즈용 Key Hash를 얻을 수 있다. 아래의 '<Release key alias>' 에는 릴리즈 키의 alias를, '<Release key path>' 에는 릴리즈 키의 path를 입력한다.
> 주의 : 릴리즈용 키를 얻으려면 이전에 지정한 키 저장소 비밀번호 입력이 필요하다.

```
keytool -exportcert -alias <Release key alias> -keystore <Release key path> | openssl sha1 -binary | openssl base64
```

#### (2) 코드를 이용해서 생성하는 법
- 제공하는 sdk 내의 `BuzzStore.getKeyHash(Context context)` 함수를 이용한다. 이 함수는 앱의 context 를 파라미터로 받아 현재 빌드된 앱의 Key Hash를 리턴한다.

## 2. BuzzStore server-to-server integration

보안 때문에 SDK 사용을 위해서는 인증이 필요하다. 위 API를 호출하면 유저의 토큰(i.e. 비밀번호)을 생성하여 퍼블리셔 서버로 전달한다. 퍼블리셔 서버는 해당 토큰을 받아서 퍼블리셔 DB에 저장해야 한다. 이 저장된 유저 토큰을 SDK 사용 시 함수 파라미터로 전달해야 한다. 이 API 사용을 위해서는 먼저 퍼블리셔 서버의 아이피 주소를 화이트 리스트로 등록해야 한다. 화이트 리스트 등록은 별도의 채널(e.g. 이메일)을 통해서 퍼블리셔가 전달한다.

#### (1) 생성
- *호출 시점: 유저의 최초 스토어 사용 시점 이전에 반드시 한번 *
- API 호출 방향 : 퍼블리셔 서버 -> 버즈스토어 서버
- method : `POST`
- url : `https://52.193.111.153/api/users` (테스트 환경)
- Headers : 다음의 파라미터를 담아서 요청한다.
    - `HTTP-X-BUZZVIL-APP-ID` : 사전에 발급한 퍼블리셔 앱에 부여 된 고유한 아이디.
    - `HTTP-X-BUZZVIL-API-KEY` : 서버 투 서버 API 사용을 위한 고유한 API 키
- POST 필수 파라미터 : 퍼블리셔의 유저 식별자 `publisher_user_id`
- e.g.
```JSON
{
"publisher_user_id": 1270537
}
```
- 단, 버즈스토어는 퍼블리셔 유저 식별자를 기준으로 유저를 식별하므로 해당 값은 추후 바꿀 수 없다. 두개 이상의 서로 다른 유저 식별자에 대해서 버즈스토어는 서로 다른 유저로 인식한다. 따라서, 퍼블리셔가 제공하는 유저 식별자는 절대 업데이트되지 않는 값을 권장한다. 예를들어, 추후 변경의 여지가 있는 이메일보다는 퍼블리셔 디비의 고유 식별자로 권장한다.
- 성공 시 JSON 포맷으로 `publisher_user_id`, `token` 를 리턴한다. HTTP 응답 상태 코드는 200 이다. 
- e.g.
```JSON
{
"publisher_user_id": 1270537,
"token": "yYn05pKNlHpMdmBd2GeXU4tBdQtIENuFmFJ0MhBJkwBIzE2TXffLTA7bfWAmRSOc"
}
```
- 실패 시 JSON 포맷으로 `error_code`, `error_message` 를 리턴한다. HTTP 응답 상태 코드는 400 이다.

#### (2) 재발급
- *호출 시점: 특정한 유저 A의 최신 토큰을 재발급 받을 필요가 있을 때*
- API 호출 방향 : 퍼블리셔 서버 -> 버즈스토어 서버
- method: `GET`
- url : `https://52.193.111.153/api/users/<user_id>` (테스트 환경)
- Headers : 다음의 파라미터를 담아서 요청한다.
    - `HTTP-X-BUZZVIL-APP-ID` : 사전에 발급한 퍼블리셔 앱에 부여 된 고유한 아이디.
    - `HTTP-X-BUZZVIL-API-KEY` : 서버 투 서버 API 사용을 위한 고유한 API 키

- GET 필수 파라미터 : 없음. 단, Restful API 디자인 가이드라인에 따라 url 주소 `<user_id>` 자리에 퍼블리셔 유저 식별자를 입력한다.

#### 주의사항 
- 천재지변등의 이유로 버즈스토어 서버가 위 API 를 통한 토큰 발급에 실패하는 경우 스토어 SDK 이용이 제한 된다. 
- 이 때, 퍼블리셔 서버는 주기적인 재시도를 통해 유저 토큰을 발급 받아야 한다.

## 3. Publisher API call

위의 server-to-server 연동을 통해 버즈스토어 서버로부터 퍼블리셔 서버로 유저 토큰을 전달하게 된다. 이 토큰은 스토어 SDK 사용을 위해 반드시 필요하다. 따라서, 퍼블리셔 서버에 저장 된 유저 토큰을 클라이언트로 전달하기 위해 퍼블리셔 측의 API 구현이 필요하다. 퍼블리셔 앱이 기존에 쓰고 있던 통신 방식을 통해 유저 토큰을 퍼블리셔 서버에서 클라이언트로 전달한다.
- 유저 토큰을 퍼블리셔 디비에서 클라이언트로 전달하는 시점은 로그인 성공시를 권장한다.
- SDK가 제공하는 listener를 이용하여 클라이언트가 유저 토큰 획득에 실패할 경우를 캐치하여 다시 요청을 시도해야 한다.

## 4. BuzzStore SDK for Android integration

- 버즈스토어를 안드로이드 어플리케이션에 연동하기 위한 라이브러리
- 안드로이드 버전 지원 : Android 2.3(API Level 9) 이상
- 스토어 목록은 웹뷰로 이루어져 있으며, SDK를 이용해 필수 파라미터를 전달하여 모바일 UI를 호출한다.
- 보안을 위한 user Token 관리를 위해 퍼블리셔 서버와 버즈스토어 서버 간 server to server 연동이 선행되어야 한다.

#### 필수 파라미터 설명
- `appKey` : 버즈스토어에서 퍼블리셔 앱을 구별하기 위한 고유 식별자. 고정된 값으로, 연동 전에 지급하며 어드민에서 확인할 수 있다.
- `userId` : 퍼블리셔에서 관리하는 고유의 유저 아이디.
- `userToken` : userId와 1:1매칭되는 보안용 토큰. 회원 가입 완료 시 2단계의 server-to-server 연동을 통해 버즈스토어 서버로부터 퍼블리셔 서버로 전달 받으며, 3단계의 API call 을 통해 퍼블리셔 서버에서 퍼블리셔 앱으로 전달받는다.

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

#### 버즈스토어 모바일 UI 호출
- `BuzzStore.init(String appKey, String userId, String userToken, Context context)` : 초기화 함수로, 버즈스토어를 로드하려는 액티비티의 onCreate 에 호출한다.

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
         * appKey : Unique key value for publisher.
         * userId : user identifier used from publisher
         * userToken : latest user token from server to server communication
         * this : Context
         */
        BuzzStore.init("appKey", "userId", "userToken", this);

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

