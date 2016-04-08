package com.buzzvil.buzzstore.sample;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.buzzvil.buzzstore.sdk.BuzzStore;

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
