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
import android.widget.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private final Random rng = new Random();
    private final Locale br = new Locale("pt","BR");
    private SharedPreferences prefs;

    private long money, totalRevenue;
    private int fans, phones, year, month, reputation, tech, marketing;
    private int employees, factory, totalUnits, awards, bestScore;
    private String company, lastPhone;

    private TextView companyView, dateView, moneyView, fansView, repView, modelsView;
    private TextView marketView, employeesView, factoryView, levelsView, headlineView;
    private LinearLayout historyBox;

    private final int BG=Color.rgb(7,10,16);
    private final int PANEL=Color.rgb(18,23,34);
    private final int PANEL2=Color.rgb(27,34,48);
    private final int ACCENT=Color.rgb(112,92,255);
    private final int BLUE=Color.rgb(50,126,230);
    private final int GREEN=Color.rgb(35,166,107);
    private final int TEXT=Color.rgb(246,248,252);
    private final int MUTED=Color.rgb(163,173,194);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("phone_empire_save",MODE_PRIVATE);
        load();
        buildUi();
        refresh();
    }

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable rounded(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView tv(String s,int sp,boolean bold,int color){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.DEFAULT_BOLD); return v;
    }
    private LinearLayout card(){
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16),dp(15),dp(16),dp(15)); l.setBackground(rounded(PANEL,18));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(12)); l.setLayoutParams(p);
        return l;
    }
    private Button action(String label,int color){
        Button b=new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(15);
        b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT_BOLD); b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(color,15));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54)); p.setMargins(0,dp(5),0,dp(5)); b.setLayoutParams(p);
        return b;
    }
    private LinearLayout stat(String label, TextView[] out){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14),dp(12),dp(14),dp(12)); c.setBackground(rounded(PANEL,17));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(86),1f); p.setMargins(0,0,dp(6),dp(10)); c.setLayoutParams(p);
        c.addView(tv(label,11,true,MUTED)); TextView val=tv("—",19,true,TEXT); val.setPadding(0,dp(6),0,0); c.addView(val); out[0]=val;
        return c;
    }

    private void buildUi(){
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(24),dp(18),dp(38));

        root.addView(tv("PHONE EMPIRE",29,true,TEXT));
        TextView sub=tv("Construa a maior fabricante de celulares do mundo.",14,false,MUTED);
        sub.setPadding(0,dp(3),0,dp(17)); root.addView(sub);

        LinearLayout hero=card(); hero.setBackground(rounded(ACCENT,22));
        companyView=tv("",24,true,Color.WHITE); hero.addView(companyView);
        dateView=tv("",13,false,Color.rgb(228,224,255)); dateView.setPadding(0,dp(3),0,dp(10)); hero.addView(dateView);
        headlineView=tv("",14,true,Color.WHITE); hero.addView(headlineView);
        hero.setOnClickListener(v->renameCompany()); root.addView(hero);

        TextView[] a=new TextView[1],b=new TextView[1],c=new TextView[1],d=new TextView[1];
        LinearLayout row1=new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL); row1.setWeightSum(2);
        row1.addView(stat("CAIXA",a)); row1.addView(stat("FÃS",b)); moneyView=a[0]; fansView=b[0]; root.addView(row1);
        LinearLayout row2=new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL); row2.setWeightSum(2);
        row2.addView(stat("REPUTAÇÃO",c)); row2.addView(stat("MODELOS",d)); repView=c[0]; modelsView=d[0]; root.addView(row2);

        TextView[] e=new TextView[1],f=new TextView[1],g=new TextView[1];
        LinearLayout row3=new LinearLayout(this); row3.setOrientation(LinearLayout.HORIZONTAL); row3.setWeightSum(2);
        row3.addView(stat("MERCADO",e)); row3.addView(stat("FUNCIONÁRIOS",f)); marketView=e[0]; employeesView=f[0]; root.addView(row3);
        LinearLayout fac=card(); fac.addView(tv("🏭 Produção",17,true,TEXT)); factoryView=tv("",14,false,MUTED); factoryView.setPadding(0,dp(5),0,0); fac.addView(factoryView); root.addView(fac);

        TextView sec=tv("Decisões",20,true,TEXT); sec.setPadding(0,dp(8),0,dp(5)); root.addView(sec);
        Button create=action("📱  Projetar e lançar celular",ACCENT); create.setOnClickListener(v->openPhoneDesigner()); root.addView(create);
        Button showroom=action("🧊  Showroom 3D",Color.rgb(190,72,210)); showroom.setOnClickListener(v->startActivity(new android.content.Intent(this,Phone3DActivity.class))); root.addView(showroom);
        Button companyBtn=action("🏢  Gerenciar empresa",GREEN); companyBtn.setOnClickListener(v->openCompanyMenu()); root.addView(companyBtn);
        Button marketBtn=action("📊  Mercado e concorrentes",BLUE); marketBtn.setOnClickListener(v->openMarket()); root.addView(marketBtn);
        Button next=action("⏩  Avançar 1 mês",PANEL2); next.setOnClickListener(v->advanceMonth()); root.addView(next);

        LinearLayout levels=card(); levels.addView(tv("Visão geral",17,true,TEXT));
        levelsView=tv("",14,false,MUTED); levelsView.setPadding(0,dp(7),0,0); levels.addView(levelsView); root.addView(levels);

        TextView recent=tv("Notícias da empresa",20,true,TEXT); recent.setPadding(0,dp(9),0,dp(8)); root.addView(recent);
        historyBox=new LinearLayout(this); historyBox.setOrientation(LinearLayout.VERTICAL); root.addView(historyBox);

        Button reset=action("↺  Novo jogo",Color.rgb(115,47,57)); reset.setOnClickListener(v->confirmReset()); root.addView(reset);

        sc.addView(root); setContentView(sc);
    }

    private void openPhoneDesigner(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),0,dp(20),0);
        EditText name=new EditText(this); name.setHint("Nome do modelo"); name.setSingleLine();
        name.setText(company+" "+(phones+1)); box.addView(name);

        Spinner segment=spinner("Categoria",new String[]{"Entrada","Intermediário","Premium","Ultra"},box);
        Spinner cpu=spinner("Processador",new String[]{"Eco","Core","Pro","Xtreme"},box);
        Spinner cam=spinner("Câmera",new String[]{"12 MP","48 MP","108 MP","200 MP"},box);
        Spinner battery=spinner("Bateria",new String[]{"4000 mAh","5000 mAh","6000 mAh"},box);
        Spinner display=spinner("Tela",new String[]{"LCD 60 Hz","OLED 90 Hz","OLED 120 Hz","LTPO 144 Hz"},box);
        Spinner ram=spinner("RAM",new String[]{"4 GB","6 GB","8 GB","12 GB"},box);
        Spinner storage=spinner("Armazenamento",new String[]{"64 GB","128 GB","256 GB","512 GB"},box);

        Button preview3d=action("🧊  Ver protótipo em 3D",Color.rgb(190,72,210));
        preview3d.setOnClickListener(v->startActivity(new android.content.Intent(this,Phone3DActivity.class)));
        box.addView(preview3d);

        TextView priceLabel=tv("Preço: R$ 1.499",15,true,Color.DKGRAY); priceLabel.setPadding(0,dp(12),0,0); box.addView(priceLabel);
        SeekBar price=new SeekBar(this); price.setMax(5500); price.setProgress(999); box.addView(price);
        price.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean from){priceLabel.setText("Preço: "+money(500+p));}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });

        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Novo celular").setView(box)
                .setNegativeButton("Cancelar",null).setPositiveButton("Lançar",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->
                launchPhone(name.getText().toString().trim(),segment.getSelectedItemPosition(),cpu.getSelectedItemPosition(),
                        cam.getSelectedItemPosition(),battery.getSelectedItemPosition(),display.getSelectedItemPosition(),
                        ram.getSelectedItemPosition(),storage.getSelectedItemPosition(),500+price.getProgress(),dialog)));
        dialog.show();
    }

    private Spinner spinner(String label,String[] items,LinearLayout box){
        TextView l=tv(label,12,true,Color.DKGRAY); l.setPadding(0,dp(8),0,0); box.addView(l);
        Spinner s=new Spinner(this); s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items));
        box.addView(s); return s;
    }

    private void launchPhone(String name,int segment,int cpu,int cam,int battery,int display,int ram,int storage,int price,AlertDialog dialog){
        if(name.length()<2){toast("Dê um nome ao celular.");return;}
        int score=28 + cpu*10 + cam*7 + battery*5 + display*8 + ram*6 + storage*4 + tech*5;
        score=Math.min(100,score);

        long devCost=14000L + segment*6500L + cpu*7500L + cam*5000L + display*6000L + ram*3500L + storage*3000L;
        devCost+=phones*1800L;
        if(money<devCost){toast("Desenvolvimento custa "+money(devCost));return;}

        int capacity=factory*2400 + employees*90;
        int segmentBase[]={950,1500,2600,4000};
        int target=segmentBase[segment];
        double value=(target + score*24.0)/(double)Math.max(500,price);
        value=Math.max(0.30,Math.min(1.65,value));
        int demand=700 + fans/10 + reputation*35 + marketing*210 + rng.nextInt(900);
        demand+=(segment==1?450:segment==2?200:0);
        int units=Math.max(80,(int)(demand*value));
        boolean capped=units>capacity; units=Math.min(units,capacity);

        long unitCost=190L + cpu*55L + cam*38L + battery*25L + display*48L + ram*34L + storage*28L;
        unitCost=Math.max(160,unitCost-tech*8L);
        long production=unitCost*units;
        long revenue=(long)price*units;
        long profit=revenue-production-devCost;

        money+=profit; totalRevenue+=revenue; totalUnits+=units; phones++; lastPhone=name;
        int fanGain=Math.max(120,units/3 + score*18 - Math.max(0,price-target)/10);
        fans=Math.max(0,fans+fanGain);
        reputation=Math.min(100,reputation + 1 + score/20);
        if(score>bestScore)bestScore=score;

        String result="Nota "+score+"/100 • "+units+" vendidos • resultado "+money(profit);
        if(capped)result+=" • fábrica no limite";
        addHistory("📱 "+name,result);
        checkAwards();
        dialog.dismiss(); save(); refresh();
    }

    private void openCompanyMenu(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(4),dp(18),0);
        TextView info=tv("Funcionários: "+employees+"\nFábrica: nível "+factory+"\nTecnologia: nível "+tech+"\nMarketing: nível "+marketing,15,false,Color.DKGRAY);
        box.addView(info);

        Button hire=action("👥 Contratar funcionário • "+money(hireCost()),GREEN);
        hire.setOnClickListener(v->{long c=hireCost();if(money<c){toast("Caixa insuficiente.");return;}money-=c;employees++;addHistory("👥 Nova contratação","Equipe agora tem "+employees+" funcionários.");save();refresh();info.setText("Funcionários: "+employees+"\nFábrica: nível "+factory+"\nTecnologia: nível "+tech+"\nMarketing: nível "+marketing);});
        box.addView(hire);

        Button fac=action("🏭 Ampliar fábrica • "+money(factoryCost()),Color.rgb(214,128,35));
        fac.setOnClickListener(v->{long c=factoryCost();if(money<c){toast("Caixa insuficiente.");return;}money-=c;factory++;reputation=Math.min(100,reputation+2);addHistory("🏭 Fábrica ampliada","Capacidade mensal subiu para "+capacity()+" unidades.");save();refresh();});
        box.addView(fac);

        Button research=action("🔬 P&D • "+money(researchCost()),Color.rgb(139,75,201));
        research.setOnClickListener(v->{long c=researchCost();if(money<c){toast("Caixa insuficiente.");return;}money-=c;tech++;reputation=Math.min(100,reputation+2);addHistory("🔬 Tecnologia nível "+tech,"Seus próximos aparelhos ganham mais qualidade e menor custo.");save();refresh();});
        box.addView(research);

        Button ad=action("📣 Marketing • "+money(marketingCost()),BLUE);
        ad.setOnClickListener(v->{long c=marketingCost();if(money<c){toast("Caixa insuficiente.");return;}money-=c;marketing++;int gain=900+rng.nextInt(2100)+marketing*500;fans+=gain;addHistory("📣 Campanha nacional","+"+gain+" fãs e marketing nível "+marketing+".");save();refresh();});
        box.addView(ad);

        new AlertDialog.Builder(this).setTitle("Gerenciar empresa").setView(box).setPositiveButton("Fechar",null).show();
    }

    private void openMarket(){
        String leader;
        double my=marketShare();
        if(my<2)leader="Titan Mobile domina o mercado com aparelhos populares.";
        else if(my<8)leader="Você começou a incomodar as marcas médias.";
        else if(my<20)leader="Sua empresa já disputa espaço entre as gigantes.";
        else leader="Sua marca virou uma das líderes do setor.";

        String msg="Sua participação estimada: "+String.format(br,"%.1f%%",my)+"\n\n"+
                "Concorrentes:\n• Titan Mobile — preço agressivo\n• NovaTech — câmeras fortes\n• Orbit — foco premium\n\n"+
                leader+"\n\nMelhor celular já lançado: "+(lastPhone.length()>0?lastPhone:"nenhum")+
                "\nMelhor nota: "+bestScore+"/100";
        new AlertDialog.Builder(this).setTitle("Mercado").setMessage(msg).setPositiveButton("OK",null).show();
    }

    private void advanceMonth(){
        month++; if(month>12){month=1;year++; yearlyReview();}

        long payroll=employees*1200L;
        long factoryCost=factory*1800L;
        long rdCost=tech*350L;
        long ops=payroll+factoryCost+rdCost;

        long catalog=phones==0?0:phones*(550L+reputation*22L+tech*180L+marketing*120L)+rng.nextInt(Math.max(1,phones*900+1));
        money+=catalog-ops;
        totalRevenue+=catalog;
        if(phones>0)fans+=40+rng.nextInt(120)+reputation;

        String note="Catálogo "+money(catalog)+" • custos "+money(ops);
        addHistory("📅 "+monthName(month)+" de "+year,note);
        randomEvent();
        save(); refresh();
    }

    private void yearlyReview(){
        int score=(int)Math.min(100,reputation*0.45 + Math.min(35,marketShare()*1.5) + Math.min(20,bestScore/5.0));
        if(score>=65){awards++;reputation=Math.min(100,reputation+4);fans+=2500;addHistory("🏆 Prêmio do ano","A "+company+" ganhou destaque da imprensa. +2.500 fãs.");}
        else addHistory("📈 Fechamento anual","A empresa terminou "+(year-1)+" com nota corporativa "+score+"/100.");
    }

    private void randomEvent(){
        if(rng.nextInt(100)>=32)return;
        int event=rng.nextInt(5);
        if(event==0 && phones>0){int gain=500+rng.nextInt(1800);fans+=gain;reputation=Math.min(100,reputation+2);addHistory("🔥 Review viral","Um aparelho viralizou nas redes. +"+gain+" fãs.");}
        else if(event==1){long loss=2500L+rng.nextInt(6000);money-=loss;addHistory("🚚 Problema de fornecedores","Atrasos custaram "+money(loss)+".");}
        else if(event==2){long bonus=3500L+rng.nextInt(8000);money+=bonus;addHistory("🤝 Contrato empresarial","Novo contrato trouxe "+money(bonus)+".");}
        else if(event==3 && reputation>5){reputation--;addHistory("⚠️ Reclamações de clientes","A reputação caiu 1 ponto neste mês.");}
        else {int gain=150+rng.nextInt(650);fans+=gain;addHistory("📸 Marca em destaque","Sua campanha apareceu em páginas de tecnologia. +"+gain+" fãs.");}
    }

    private void checkAwards(){
        if(phones==1)addHistory("🎯 Conquista","Primeiro celular lançado!");
        if(totalUnits>=10000 && !prefs.getBoolean("award10k",false)){awards++;prefs.edit().putBoolean("award10k",true).apply();addHistory("🏅 10 mil unidades","Sua empresa ultrapassou 10.000 celulares vendidos.");}
        if(fans>=50000 && !prefs.getBoolean("award50k",false)){awards++;prefs.edit().putBoolean("award50k",true).apply();addHistory("🌟 Marca famosa","Você chegou a 50 mil fãs.");}
        if(bestScore>=90 && !prefs.getBoolean("award90",false)){awards++;prefs.edit().putBoolean("award90",true).apply();addHistory("💎 Celular lendário","Você lançou um aparelho com nota 90+.");}
    }

    private void renameCompany(){
        EditText input=new EditText(this); input.setText(company); input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(this).setTitle("Nome da empresa").setView(input).setNegativeButton("Cancelar",null)
                .setPositiveButton("Salvar",(d,w)->{String n=input.getText().toString().trim();if(n.length()>=2){company=n;save();refresh();}}).show();
    }

    private void confirmReset(){
        new AlertDialog.Builder(this).setTitle("Começar do zero?").setMessage("Todo o progresso do Phone Empire será apagado.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Novo jogo",(d,w)->{prefs.edit().clear().apply();loadDefaults();save();recreate();}).show();
    }

    private void refresh(){
        companyView.setText(company+"  ›");
        dateView.setText(monthName(month)+" de "+year+" • toque para renomear");
        headlineView.setText(companySize()+"  •  "+awards+" prêmio"+(awards==1?"":"s"));
        moneyView.setText(money(money)); fansView.setText(shortNumber(fans)); repView.setText(reputation+"/100"); modelsView.setText(String.valueOf(phones));
        marketView.setText(String.format(br,"%.1f%%",marketShare())); employeesView.setText(String.valueOf(employees));
        factoryView.setText("Nível "+factory+" • capacidade "+shortNumber(capacity())+" unidades/mês");
        levelsView.setText("🔬 Tecnologia: nível "+tech+"\n📣 Marketing: nível "+marketing+
                "\n📦 Unidades vendidas: "+shortNumber(totalUnits)+"\n💰 Receita total: "+money(totalRevenue)+
                "\n⭐ Melhor aparelho: "+bestScore+"/100");
        renderHistory();
    }

    private void renderHistory(){
        historyBox.removeAllViews();
        String h=prefs.getString("history","");
        if(h.length()==0){
            LinearLayout c=card(); c.addView(tv("Bem-vindo, CEO",16,true,TEXT));
            TextView s=tv("Você tem "+money(money)+". Contrate, pesquise e lance seu primeiro celular.",13,false,MUTED); s.setPadding(0,dp(4),0,0); c.addView(s); historyBox.addView(c); return;
        }
        String[] entries=h.split("\\|\\|ENTRY\\|\\|");
        int start=Math.max(0,entries.length-7);
        for(int i=entries.length-1;i>=start;i--){
            String[] p=entries[i].split("\\|\\|SUB\\|\\|",2); if(p.length<2)continue;
            LinearLayout c=card(); c.addView(tv(p[0],16,true,TEXT));
            TextView s=tv(p[1],13,false,MUTED); s.setPadding(0,dp(4),0,0); c.addView(s); historyBox.addView(c);
        }
    }

    private void addHistory(String title,String sub){
        String h=prefs.getString("history","");
        String e=title+"||SUB||"+sub;
        if(h.length()>0)h+="||ENTRY||";
        h+=e;
        String[] a=h.split("\\|\\|ENTRY\\|\\|");
        if(a.length>40){
            StringBuilder b=new StringBuilder();
            for(int i=a.length-40;i<a.length;i++){if(b.length()>0)b.append("||ENTRY||");b.append(a[i]);}
            h=b.toString();
        }
        prefs.edit().putString("history",h).apply();
    }

    private long hireCost(){return 2500L+employees*900L;}
    private long factoryCost(){return 28000L*factory;}
    private long researchCost(){return 14000L*(tech+1);}
    private long marketingCost(){return 7500L*(marketing+1);}
    private int capacity(){return factory*2400+employees*90;}
    private double marketShare(){return Math.min(38.0,0.2+phones*0.35+reputation*0.055+fans/14000.0+awards*0.9);}
    private String companySize(){if(phones<2)return "Startup";if(phones<5)return "Marca emergente";if(phones<10)return "Fabricante nacional";if(phones<18)return "Gigante global";return "Império tecnológico";}
    private String monthName(int m){String[] a={"Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"};return a[Math.max(1,Math.min(12,m))-1];}
    private String money(long n){return NumberFormat.getCurrencyInstance(br).format(n);}
    private String shortNumber(long n){if(n>=1000000)return String.format(br,"%.1f mi",n/1000000.0);if(n>=1000)return String.format(br,"%.1f mil",n/1000.0);return String.valueOf(n);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private void load(){
        if(!prefs.contains("money")){loadDefaults();return;}
        money=prefs.getLong("money",100000); fans=prefs.getInt("fans",0); phones=prefs.getInt("phones",0);
        year=prefs.getInt("year",2026); month=prefs.getInt("month",1); reputation=prefs.getInt("reputation",8);
        tech=prefs.getInt("tech",0); marketing=prefs.getInt("marketing",0); company=prefs.getString("company","Nox Mobile");
        employees=prefs.getInt("employees",3); factory=prefs.getInt("factory",1); totalUnits=prefs.getInt("totalUnits",0);
        awards=prefs.getInt("awards",0); bestScore=prefs.getInt("bestScore",0); totalRevenue=prefs.getLong("totalRevenue",0);
        lastPhone=prefs.getString("lastPhone","");
    }

    private void loadDefaults(){
        money=100000; fans=0; phones=0; year=2026; month=1; reputation=8; tech=0; marketing=0;
        employees=3; factory=1; totalUnits=0; awards=0; bestScore=0; totalRevenue=0; company="Nox Mobile"; lastPhone="";
    }

    private void save(){
        prefs.edit().putLong("money",money).putInt("fans",fans).putInt("phones",phones)
                .putInt("year",year).putInt("month",month).putInt("reputation",reputation)
                .putInt("tech",tech).putInt("marketing",marketing).putString("company",company)
                .putInt("employees",employees).putInt("factory",factory).putInt("totalUnits",totalUnits)
                .putInt("awards",awards).putInt("bestScore",bestScore).putLong("totalRevenue",totalRevenue)
                .putString("lastPhone",lastPhone).apply();
    }
}
