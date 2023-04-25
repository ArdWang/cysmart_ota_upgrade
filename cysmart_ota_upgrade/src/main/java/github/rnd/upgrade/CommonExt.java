package github.rnd.upgrade;


import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;




/**
 * Created by rnd on 2018/4/9.
 * 扩展一些方法
 */

public class CommonExt {
    /**
     * 扩展方法 判断按钮是否可以点击
     * @param et
     * @param btn
     * @param a
     */
    public static void buttonEnable(EditText et, final Button btn, final boolean a){

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(a) {
                    btn.setEnabled(true);
                }else{
                    btn.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * 弹出消息框
     * @param msg
     */
    public static void toast(Context context, String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }

    /**
     ImageView加载网络图片
    */
    public static void loadUrl(String url, ImageView imageView) {
       // GlideUtils.loadUrlImage(BaseApplication.getContext(), url, imageView);
    }

    /**
     扩展视图可见性
     */
    public static void setVisible (View view, Boolean visible){
        if(visible){
            view.setVisibility(View.VISIBLE);
        }else{
            view.setVisibility(View.GONE);
        }

    }
}
