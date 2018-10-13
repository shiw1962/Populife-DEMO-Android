package com.example.ttlock.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.ttlock.R;
import com.example.ttlock.databinding.ActivityGatewayBinding;
import com.example.ttlock.net.ResponseService;
import com.ttlock.bl.sdk.util.LogUtil;
import com.ttlock.bl.sdk.util.NetworkUtil;
import com.ttlock.gateway.sdk.api.GatewayAPI;
import com.ttlock.gateway.sdk.callback.GatewayCallback;

import org.json.JSONObject;

public class GatewayActivity extends BaseActivity {

    ActivityGatewayBinding binding;

    /**
     * wifi SSID
     */
    private String wifiName;

    /**
     * wifi password
     */
    private String wifiPwd;

    /**
     * user password
     */
    private String userPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_gateway);
        binding.setWifiName(NetworkUtil.getWifiSSid(this));
    }

    public void onClick(View view) {
        wifiName = binding.wifiName.getText().toString().trim();
        wifiPwd = binding.password.getText().toString().trim();
        userPwd = binding.userPwd.getText().toString().trim();

        if(TextUtils.isEmpty(wifiPwd) || TextUtils.isEmpty(wifiName) || TextUtils.isEmpty(userPwd)) {
            Toast.makeText(this, getString(R.string.words_check_input), Toast.LENGTH_LONG).show();
            return;
        }

        showProgressDialog();
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                String json = ResponseService.getUserId();
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    if (jsonObject.has("errcode")) {
                        cancelProgressDialog();
                        String errmsg = jsonObject.getString("errmsg");
                        toast(errmsg);
                    } else {
                        int uid = jsonObject.getInt("uid");
                        //Regist gateway interface
                        GatewayAPI gatewayAPI = new GatewayAPI(GatewayActivity.this, new GatewayCallback() {
                            @Override
                            public void onConnectTimeOut() {
                                cancelProgressDialog();
                                toast(getString(R.string.words_time_out));
                            }

                            @Override
                            public void onConnectOk(String mac) {
                                cancelProgressDialog();
                                String json = ResponseService.isInitSuccess(mac);
                                LogUtil.d("json:" + json, DBG);
                                try {
                                    JSONObject jsonObject = new JSONObject(json);
                                    int errcode = jsonObject.getInt("errcode");
                                    if (errcode != 0) {
                                        String errmsg = jsonObject.getString("errmsg");
                                        toast(errmsg);
                                    } else {
                                        toast(getString(R.string.words_gateway_init_successed));
                                        Intent intent = new Intent(GatewayActivity.this, GatewayListActivity.class);
                                        startActivity(intent);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        //start link
                        gatewayAPI.startConnectLink(uid, userPwd, wifiName, wifiPwd);
                    }
                } catch (Exception e) {

                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer uid) {
                super.onPostExecute(uid);
            }
        }.execute();
    }
}
