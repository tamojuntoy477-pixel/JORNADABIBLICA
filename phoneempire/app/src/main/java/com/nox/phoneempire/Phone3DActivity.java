package com.nox.phoneempire;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

public class Phone3DActivity extends Activity {
    private Phone3DView view;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private Button colorBtn(String text,int color){
        Button b=new Button(this);b.setText(text);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setTextSize(13);b.setBackground(bg(color,12));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.setMargins(dp(3),0,dp(3),0);b.setLayoutParams(p);return b;
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(7,10,16));
        getWindow().setNavigationBarColor(Color.rgb(7,10,16));

        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(18),dp(14),dp(18));root.setBackgroundColor(Color.rgb(7,10,16));
        TextView title=new TextView(this);title.setText("SHOWROOM 3D");title.setTextColor(Color.WHITE);title.setTextSize(25);title.setGravity(Gravity.CENTER_HORIZONTAL);title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);root.addView(title);
        TextView hint=new TextView(this);hint.setText("Arraste o dedo para girar o celular");hint.setTextColor(Color.rgb(170,178,198));hint.setTextSize(14);hint.setGravity(Gravity.CENTER_HORIZONTAL);hint.setPadding(0,dp(3),0,dp(8));root.addView(hint);

        view=new Phone3DView(this);
        LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,0,1);vp.setMargins(0,dp(4),0,dp(12));view.setLayoutParams(vp);view.setBackground(bg(Color.rgb(12,17,27),20));root.addView(view);

        TextView label=new TextView(this);label.setText("Cor da carcaça");label.setTextColor(Color.WHITE);label.setTextSize(15);label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);label.setPadding(0,0,0,dp(7));root.addView(label);

        LinearLayout colors=new LinearLayout(this);colors.setOrientation(LinearLayout.HORIZONTAL);
        Button black=colorBtn("Preto",Color.rgb(45,45,52));black.setOnClickListener(v->view.setBodyColor(.16f,.16f,.19f));colors.addView(black);
        Button violet=colorBtn("Violeta",Color.rgb(112,92,255));violet.setOnClickListener(v->view.setBodyColor(.44f,.36f,1f));colors.addView(violet);
        Button blue=colorBtn("Azul",Color.rgb(39,112,210));blue.setOnClickListener(v->view.setBodyColor(.12f,.36f,.72f));colors.addView(blue);
        Button silver=colorBtn("Prata",Color.rgb(125,132,145));silver.setOnClickListener(v->view.setBodyColor(.56f,.59f,.64f));colors.addView(silver);
        root.addView(colors);

        Button auto=new Button(this);auto.setText("⏸ Parar rotação automática");auto.setTextColor(Color.WHITE);auto.setAllCaps(false);auto.setTextSize(14);auto.setBackground(bg(Color.rgb(27,34,48),14));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(52));ap.setMargins(0,dp(10),0,0);auto.setLayoutParams(ap);
        auto.setOnClickListener(v->{boolean next=!view.isAutoRotate();view.setAutoRotate(next);auto.setText(next?"⏸ Parar rotação automática":"▶ Rotação automática");});
        root.addView(auto);

        setContentView(root);
    }
    @Override protected void onPause(){super.onPause();view.onPause();}
    @Override protected void onResume(){super.onResume();if(view!=null)view.onResume();}
}
