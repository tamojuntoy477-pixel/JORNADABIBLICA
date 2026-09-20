package com.nox.phoneempire;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private final Random random = new Random();
    private final Locale br = new Locale("pt", "BR");
    private SharedPreferences prefs;
    private long money;
    private int fans, phones, year, month, reputation, tech, marketing;
    private String company;
    private TextView moneyView, fansView, repView, dateView, companyView;
    private LinearLayout historyBox;
    private final int bg=Color.rgb(8,11,18), panel=Color.rgb(18,23,34), panel2=Color.rgb(27,34,48);
    private final int accent=Color.rgb(109,94,252), text=Color.rgb(245,247,252), muted=Color.rgb(164,173,194);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        prefs=getSharedPreferences("phone_empire_save",MODE_PRIVATE);
        load(); buildUi(); refresh();
    }

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private TextView tv(String s,int sp,boolean bold,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(bold)v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(15),dp(16),dp(15));l.setBackground(rounded(panel,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(12));l.setLayoutParams(p);return l;}
    private Button action(String label,int color){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT_BOLD);b.setGravity(Gravity.CENTER);b.setBackground(rounded(color,15));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));p.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(p);return b;}

    private void buildUi(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(24),dp(18),dp(36));
        root.addView(tv("PHONE EMPIRE",28,true,text));
        TextView tag=tv("Construa sua marca. Domine o mercado.",14,false,muted);tag.setPadding(0,dp(3),0,dp(18));root.addView(tag);

        LinearLayout cc=card();cc.setBackground(rounded(accent,20));companyView=tv("",24,true,Color.WHITE);cc.addView(companyView);dateView=tv("",14,false,Color.rgb(225,222,255));cc.addView(dateView);cc.setOnClickListener(v->renameCompany());root.addView(cc);

        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);r1.setWeightSum(2);
        LinearLayout a=miniStat("CAIXA");moneyView=(TextView)a.getChildAt(1);
        LinearLayout b=miniStat("FÃS");fansView=(TextView)b.getChildAt(1);r1.addView(a);r1.addView(b);root.addView(r1);

        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);r2.setWeightSum(2);
        LinearLayout c=miniStat("REPUTAÇÃO");repView=(TextView)c.getChildAt(1);
        LinearLayout d=miniStat("MODELOS");((TextView)d.getChildAt(1)).setTag("models");r2.addView(c);r2.addView(d);root.addView(r2);

        TextView sec=tv("Sua empresa",20,true,text);sec.setPadding(0,dp(12),0,dp(6));root.addView(sec);
        Button create=action("📱  Projetar novo celular",accent);create.setOnClickListener(v->openPhoneDesigner());root.addView(create);
        Button ad=action("📣  Campanha de marketing",Color.rgb(37,126,224));ad.setOnClickListener(v->marketingCampaign());root.addView(ad);
        Button rnd=action("🔬  Pesquisa e desenvolvimento",Color.rgb(141,68,196));rnd.setOnClickListener(v->research());root.addView(rnd);
        Button next=action("⏩  Avançar 1 mês",panel2);next.setOnClickListener(v->advanceMonth());root.addView(next);

        LinearLayout lv=card();lv.addView(tv("Níveis da empresa",17,true,text));TextView lvt=tv("",14,false,muted);lvt.setTag("levels");lvt.setPadding(0,dp(7),0,0);lv.addView(lvt);root.addView(lv);

        TextView recent=tv("Últimos acontecimentos",20,true,text);recent.setPadding(0,dp(12),0,dp(8));root.addView(recent);
        historyBox=new LinearLayout(this);historyBox.setOrientation(LinearLayout.VERTICAL);root.addView(historyBox);

        Button reset=action("↺  Recomeçar império",Color.rgb(120,45,54));reset.setOnClickListener(v->confirmReset());root.addView(reset);
        sc.addView(root);setContentView(sc);
    }

    private LinearLayout miniStat(String label){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(rounded(panel,17));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(86),1);p.setMargins(0,0,dp(6),dp(10));c.setLayoutParams(p);
        c.addView(tv(label,11,true,muted));TextView v=tv("—",20,true,text);v.setPadding(0,dp(6),0,0);c.addView(v);return c;
    }

    private void openPhoneDesigner(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(4),dp(20),0);
        EditText model=new EditText(this);model.setHint("Nome do modelo");model.setText(company+" One "+(phones+1));model.setSingleLine();box.addView(model);
        Spinner chip=spinner("Processador",new String[]{"Básico","Intermediário","Potente","Ultra"},box);
        Spinner cam=spinner("Câmera",new String[]{"12 MP","48 MP","64 MP","108 MP"},box);
        Spinner bat=spinner("Bateria",new String[]{"4000 mAh","5000 mAh","6000 mAh"},box);
        Spinner ram=spinner("RAM",new String[]{"4 GB","6 GB","8 GB","12 GB"},box);
        Spinner storage=spinner("Armazenamento",new String[]{"64 GB","128 GB","256 GB","512 GB"},box);
        TextView priceLabel=tv("Preço: R$ 1.499",15,true,Color.DKGRAY);priceLabel.setPadding(0,dp(12),0,0);box.addView(priceLabel);
        SeekBar price=new SeekBar(this);price.setMax(4500);price.setProgress(999);box.addView(price);
        price.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){priceLabel.setText("Preço: "+money(500+p));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Criar novo celular").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Lançar",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->launchPhone(model.getText().toString().trim(),chip.getSelectedItemPosition(),cam.getSelectedItemPosition(),bat.getSelectedItemPosition(),ram.getSelectedItemPosition(),storage.getSelectedItemPosition(),500+price.getProgress(),dialog)));
        dialog.show();
    }

    private Spinner spinner(String label,String[] items,LinearLayout box){
        TextView l=tv(label,12,true,Color.DKGRAY);l.setPadding(0,dp(8),0,0);box.addView(l);
        Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items));box.addView(s);return s;
    }

    private void launchPhone(String name,int chip,int cam,int bat,int ram,int storage,int price,AlertDialog dialog){
        if(name.length()<2){toast("Dê um nome ao celular.");return;}
        int score=chip*6+cam*4+bat*3+ram*4+storage*3+tech*4;
        long development=18000L+chip*9000L+cam*6000L+bat*4500L+ram*5500L+storage*4500L+phones*3500L;
        if(money<development){toast("Você precisa de "+money(development)+" para desenvolver esse modelo.");return;}
        money-=development;phones++;
        int demand=650+fans/9+marketing*280+reputation*120+random.nextInt(1000);
        double value=Math.max(.35,Math.min(1.5,(1200+score*42.0)/price));
        int units=Math.max(120,(int)(demand*value));
        long production=units*(230L+score*7L),revenue=(long)units*price,profit=revenue-production;
        money+=profit;int gained=Math.max(100,units/3+score*25);fans+=gained;reputation=Math.min(100,reputation+2+score/14);
        addHistory("📱 "+name,"Vendeu "+units+" unidades • lucro "+money(profit)+" • +"+gained+" fãs");
        dialog.dismiss();save();refresh();
    }

    private void marketingCampaign(){
        long cost=8000L+marketing*5000L;if(money<cost){toast("Marketing custa "+money(cost));return;}
        money-=cost;int gain=1800+random.nextInt(3400)+marketing*850;fans+=gain;marketing++;
        addHistory("📣 Campanha viral","+"+gain+" fãs • marketing nível "+marketing);save();refresh();
    }

    private void research(){
        long cost=14000L+tech*9000L;if(money<cost){toast("Pesquisa custa "+money(cost));return;}
        money-=cost;tech++;reputation=Math.min(100,reputation+3);
        addHistory("🔬 Tecnologia melhorada","P&D agora está no nível "+tech+". Seus próximos celulares serão melhores.");save();refresh();
    }

    private void advanceMonth(){
        month++;if(month>12){month=1;year++;}
        long legacy=phones*(1200L+reputation*28L+random.nextInt(2200)),upkeep=1800L+phones*650L+tech*400L+marketing*300L;
        money+=legacy-upkeep;if(phones>0)fans+=80+random.nextInt(260)+reputation*2;
        addHistory("📅 "+monthName(month)+" de "+year,"Receita antiga "+money(legacy)+" • custos "+money(upkeep));save();refresh();
    }

    private void renameCompany(){
        EditText input=new EditText(this);input.setText(company);input.setSelectAllOnFocus(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(this).setTitle("Nome da empresa").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{String n=input.getText().toString().trim();if(n.length()>=2){company=n;save();refresh();}}).show();
    }

    private void confirmReset(){
        new AlertDialog.Builder(this).setTitle("Recomeçar Phone Empire?").setMessage("Seu progresso atual será apagado.").setNegativeButton("Cancelar",null).setPositiveButton("Recomeçar",(d,w)->{prefs.edit().clear().apply();loadDefaults();save();recreate();}).show();
    }

    private void refresh(){
        companyView.setText(company+"  ›");dateView.setText(monthName(month)+" de "+year+" • toque para renomear");
        moneyView.setText(money(money));fansView.setText(shortNumber(fans));repView.setText(reputation+"/100");
        updateTagged(getWindow().getDecorView(),"models",String.valueOf(phones));
        updateTagged(getWindow().getDecorView(),"levels","🔬 Tecnologia: nível "+tech+"\n📣 Marketing: nível "+marketing+"\n🏢 Tamanho: "+companySize());
        renderHistory();
    }

    private void updateTagged(View v,String tag,String value){
        if(tag.equals(v.getTag())){((TextView)v).setText(value);return;}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)updateTagged(g.getChildAt(i),tag,value);}
    }

    private void renderHistory(){
        historyBox.removeAllViews();String h=prefs.getString("history","");
        if(h.isEmpty()){LinearLayout c=card();c.addView(tv("Bem-vindo ao Phone Empire",16,true,text));TextView s=tv("Você começou com R$ 100.000. Crie seu primeiro celular!",13,false,muted);s.setPadding(0,dp(4),0,0);c.addView(s);historyBox.addView(c);return;}
        String[] e=h.split("\\|\\|ENTRY\\|\\|");int start=Math.max(0,e.length-6);
        for(int i=e.length-1;i>=start;i--){String[] p=e[i].split("\\|\\|SUB\\|\\|",2);if(p.length<2)continue;LinearLayout c=card();c.addView(tv(p[0],16,true,text));TextView s=tv(p[1],13,false,muted);s.setPadding(0,dp(4),0,0);c.addView(s);historyBox.addView(c);}
    }

    private void addHistory(String title,String sub){
        String h=prefs.getString("history",""),e=title+"||SUB||"+sub;if(!h.isEmpty())h+="||ENTRY||";h+=e;
        String[] a=h.split("\\|\\|ENTRY\\|\\|");if(a.length>30){StringBuilder b=new StringBuilder();for(int i=a.length-30;i<a.length;i++){if(b.length()>0)b.append("||ENTRY||");b.append(a[i]);}h=b.toString();}
        prefs.edit().putString("history",h).apply();
    }

    private String companySize(){if(phones<2)return "Startup de garagem";if(phones<5)return "Empresa pequena";if(phones<10)return "Marca nacional";if(phones<20)return "Gigante global";return "Império tecnológico";}
    private String monthName(int m){String[] a={"Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"};return a[Math.max(1,Math.min(12,m))-1];}
    private String money(long n){return NumberFormat.getCurrencyInstance(br).format(n);}
    private String shortNumber(long n){if(n>=1000000)return String.format(br,"%.1f mi",n/1000000.0);if(n>=1000)return String.format(br,"%.1f mil",n/1000.0);return String.valueOf(n);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void load(){if(!prefs.contains("money"))loadDefaults();else{money=prefs.getLong("money",100000);fans=prefs.getInt("fans",0);phones=prefs.getInt("phones",0);year=prefs.getInt("year",2026);month=prefs.getInt("month",1);reputation=prefs.getInt("reputation",8);tech=prefs.getInt("tech",0);marketing=prefs.getInt("marketing",0);company=prefs.getString("company","Nox Mobile");}}
    private void loadDefaults(){money=100000;fans=0;phones=0;year=2026;month=1;reputation=8;tech=0;marketing=0;company="Nox Mobile";}
    private void save(){prefs.edit().putLong("money",money).putInt("fans",fans).putInt("phones",phones).putInt("year",year).putInt("month",month).putInt("reputation",reputation).putInt("tech",tech).putInt("marketing",marketing).putString("company",company).apply();}
}
