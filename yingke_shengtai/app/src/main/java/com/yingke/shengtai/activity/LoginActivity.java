package com.yingke.shengtai.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.google.gson.reflect.TypeToken;
import com.yingke.shengtai.MyApplication;
import com.yingke.shengtai.api.IApi;
import com.yingke.shengtai.db.User;
import com.yingke.shengtai.moudle.UserDao;
import com.yingke.shengtai.moudle.UserInforData;
import com.yingke.shengtai.R;
import com.yingke.shengtai.utils.Constant;
import com.yingke.shengtai.utils.DemoHXSDKHelper;
import com.yingke.shengtai.utils.HXSDKHelper;
import com.yingke.shengtai.utils.JosnUtil;
import com.yingke.shengtai.utils.MethodUtils;
import com.yingke.shengtai.view.TitleView;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yanyiheng on 15-8-26.
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener{
    private TextView textLogin, textZhuCe;
    private EditText editPhone, editSecreate;
    private ProgressDialog dialog;
    private TitleView titleView;

    private Type type;
    private UserInforData data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initUi();
    }

    private void initUi() {
        titleView = (TitleView)findViewById(R.id.fragment_title);
        textLogin = (TextView)findViewById(R.id.login);
        textZhuCe = (TextView)findViewById(R.id.zhuce);

        editPhone = (EditText)findViewById(R.id.editPhone);
        editSecreate = (EditText)findViewById(R.id.editsecreste);

        textLogin.setOnClickListener(this);
        textZhuCe.setOnClickListener(this);
        findViewById(R.id.lookpassword).setOnClickListener(this);

        dialog = new ProgressDialog(this);
        dialog.setMessage("登录中...");

        titleView.setTitleView(R.string.login_text);
        type = new TypeToken<UserInforData>() {}.getType();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.zhuce:
                Intent intent = new Intent(this, RegistActivity.class);
                startActivity(intent);
                break;
            case R.id.login:
                if(!MethodUtils.isHasNet(this)){
                    MethodUtils.showToast(this, getResources().getString(R.string.no_net), Toast.LENGTH_SHORT);
                    break;
                }
                String phone = editPhone.getText().toString().trim();
                if(TextUtils.isEmpty(phone) || !phone.startsWith("1") || phone.length() != 11){
                    MethodUtils.showToast(this, getResources().getString(R.string.phone_cant_null), Toast.LENGTH_SHORT);
                    break;
                }
                String code = editSecreate.getText().toString().trim();
                if(TextUtils.isEmpty(code)){
                    MethodUtils.showToast(this, getResources().getString(R.string.secreate_cant_null), Toast.LENGTH_SHORT);
                    break;

                }
                if(dialog != null){
                    dialog.show();
                }
                sendData(phone, code, 1);
                break;
            case R.id.lookpassword:
                Intent intents = new Intent(this, LookPwActivity.class);
                startActivity(intents);
                break;

        }
    }

    private void showDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("确认找回密码？");
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                builder.setCancelable(true);
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
               sendDuanxin();

            }

        });
    }


    private void sendDuanxin() {
        if(!MethodUtils.isHasNet(this)){
            MethodUtils.showToast(this, "暂无网络！", Toast.LENGTH_SHORT);
            return;
        }
        MethodUtils.showToast(this, "密码已通过短信已发送,请注意查收！", Toast.LENGTH_SHORT);
        Map<String, String> map = new HashMap<String, String>();

    }

    private void sendData(String phone, String code, int i) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("mobile", phone);
        map.put("password", code);
        map.put("devicenumber", MyApplication.getInstance().getPhoneId());
        getData(IApi.NETWORK_METHOD_POST, LOGIN, IApi.URL_LOGIN,map);
    }

    @Override
    public void handleMsg(Message msg) {
        final String json = msg.getData().getString(Constant.JSON_DATA);
        if(json != null && json.length() <= 30){
            if(dialog != null && dialog.isShowing()){
                dialog.dismiss();
                MethodUtils.showToast(this, getString(R.string.login_faile), Toast.LENGTH_SHORT);
            }
            return;
        }

        switch (msg.what){
            case LOGIN:
                data = JosnUtil.gson.fromJson(json, type);


                if(!TextUtils.equals(data.getResult(), "0")){
                    if(data.getUserdetail() == null && data.getSaledetail() != null){
                        data.setUserdetail(data.getSaledetail());
                    }
                    MyApplication.getInstance().setUserInfor(data);
                    String password = "";
                    if (TextUtils.equals(MyApplication.getInstance().getUserInfor().getUserdetail().getUsertype(), "1") || TextUtils.equals(MyApplication.getInstance().getUserInfor().getUserdetail().getUsertype(), "2") || TextUtils.equals(MyApplication.getInstance().getUserInfor().getUserdetail().getUsertype(), "3") || TextUtils.equals(MyApplication.getInstance().getUserInfor().getUserdetail().getUsertype(), "0")) {
                        password = editSecreate.getText().toString();
                    } else {
                        password = Constant.NEW_PASSWORD;
                    }
                    EMChatManager.getInstance().login(MyApplication.getInstance().getUserInfor().getUserdetail().getImid(), password, new EMCallBack() {//回调
                        @Override
                        public void onSuccess() {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    try {
                                        // ** 第一次登录或者之前logout后再登录，加载所有本地群和回话
                                        // ** manually load all local groups and
//                                        EMGroupManager.getInstance().loadAllGroups();
                                        EMChatManager.getInstance().loadAllConversations();
                                        // 处理好友和群组
//                                        initializeContacts();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        // 取好友或者群聊失败，不让进入主页面
                                        runOnUiThread(new Runnable() {
                                            public void run() {
//                                                pd.dismiss();
                                                DemoHXSDKHelper.getInstance().logout(true, null);
                                                Toast.makeText(getApplicationContext(), R.string.login_failure_failed, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        return;
                                    }
                                    if(dialog != null){
                                        dialog.dismiss();
                                    }
                                    Toast.makeText(getApplicationContext(), "登录成功！", Toast.LENGTH_LONG).show();
                                    MethodUtils.setString(Constant.SHAREDREFERENCE_CONFIG_USER, Constant.SHAREDREFERENCE_CONFIG_USER, json);

                                    Intent intent = null;
                                    if (TextUtils.equals(MyApplication.getInstance().getUserInfor().getUserdetail().getUsertype(), "1") || TextUtils.equals(MyApplication.getInstance().getUserInfor().getUserdetail().getUsertype(), "2") || TextUtils.equals(MyApplication.getInstance().getUserInfor().getUserdetail().getUsertype(), "3") || TextUtils.equals(MyApplication.getInstance().getUserInfor().getUserdetail().getUsertype(), "0")) {
                                        intent = new Intent(LoginActivity.this, CustomerMainActivity.class);
                                    } else {
                                        intent = new Intent(LoginActivity.this, SaleMainActivity.class);
                                    }
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }

                        @Override
                        public void onProgress(int progress, String status) {

                        }

                        @Override
                        public void onError(int code, String message) {
                          runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  Toast.makeText(getApplicationContext(), "登陆失败,请重新登录！", Toast.LENGTH_LONG).show();
                                  if(dialog != null){
                                      dialog.dismiss();
                                  }
                              }
                          });
                        }
                    });


                } else {
                    MethodUtils.showToast(this, data.getMessage(), Toast.LENGTH_SHORT);
                    MethodUtils.setString(Constant.SHAREDREFERENCE_CONFIG_USER, Constant.SHAREDREFERENCE_CONFIG_USER, "");
                    if(dialog != null){
                        dialog.dismiss();
                    }
                }
                break;
        }

    }

    private void initializeContacts() {
        Map<String, User> userlist = new HashMap<String, User>();
        // 添加user"申请与通知"
        User newFriends = new User();
        newFriends.setUsername(Constant.NEW_FRIENDS_USERNAME);
        String strChat = getResources().getString(
                R.string.Application_and_notify);
        newFriends.setNick(strChat);

        userlist.put(Constant.NEW_FRIENDS_USERNAME, newFriends);
        // 添加"群聊"
        User groupUser = new User();
        String strGroup = getResources().getString(R.string.group_chat);
        groupUser.setUsername(Constant.GROUP_USERNAME);
        groupUser.setNick(strGroup);
        groupUser.setHeader("");
        userlist.put(Constant.GROUP_USERNAME, groupUser);

        // 添加"Robot"
        User robotUser = new User();
        String strRobot = getResources().getString(R.string.robot_chat);
        robotUser.setUsername(Constant.CHAT_ROBOT);
        robotUser.setNick(strRobot);
        robotUser.setHeader("");
        userlist.put(Constant.CHAT_ROBOT, robotUser);

        // 存入内存
        ((DemoHXSDKHelper) HXSDKHelper.getInstance()).setContactList(userlist);
        // 存入db
        UserDao dao = new UserDao(LoginActivity.this);
        List<User> users = new ArrayList<User>(userlist.values());
        dao.saveContactList(users);
    }
}