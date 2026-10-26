package com.mehekcar.rc;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 static final int GREEN=0xff126a80, GOLD=0xff58d5e6, CREAM=0xff071522, BROWN=0xff17405e, RED=0xffd34257;
 static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 BluetoothAdapter adapter; BluetoothSocket socket; OutputStream output;
 TextView status, speedLabel; Spinner devices; SeekBar speed;
 ArrayList<BluetoothDevice> paired=new ArrayList<>();
 float scale=1f;
 int theme=0;
 final String[] themes={"Midnight Cyan","Electric Neon Blue","Tarar Bari Green & Gold","Purple & Gold","Classic Royal Blue"};
 int themeColor(int c){
   int[][] pairs={
     {0xff126a80,0xff087bff,0xff375a29,0xff5b3f89,0xff0750ad},
     {0xff58d5e6,0xff32e6ff,0xffcb9e45,0xffffc66d,0xff40cfff},
     {0xff071522,0xff040e27,0xfff7efdc,0xff151020,0xff091d48},
     {0xff17405e,0xff103b85,0xff75451d,0xff48345f,0xff174eaa},
     {0xff0b2339,0xff0942a8,0xff263f1d,0xff2d2147,0xff08428e},
     {0xff10253d,0xff103e88,0xff483419,0xff392852,0xff153f8e},
     {0xff0b2236,0xff071e55,0xff314927,0xff261d3c,0xff10336e},
     {0xff12314a,0xff06398f,0xff61703c,0xff392953,0xff1254ad},
     {0xff215570,0xff159eff,0xffb99845,0xff78518c,0xff2399ec},
     {0xff142d42,0xff0b2e72,0xfff6f0e2,0xff302446,0xff0e3983},
     {0xff0b1f30,0xff081d4a,0xffe4d9bd,0xff241a39,0xff102552},
     {0xff265675,0xff238bd9,0xffcfaf61,0xff704b87,0xff2497e3},
     {0xff06364e,0xff0755ba,0xff203e22,0xff432a64,0xff1054b0},
     {0xff3095ac,0xff15bbef,0xffbba25b,0xffd79b54,0xff22a5e8},
     {0xff132d43,0xff123979,0xfff7f0df,0xff332549,0xff103b86},
     {0xff102235,0xff0b2553,0xffe2d4b4,0xff261c3b,0xff0d265b},
     {0xff26647e,0xff28a1ec,0xffb89646,0xff795d89,0xff248aca},
     {0xff102c42,0xff0b2b6d,0xffe6dbbe,0xff35274a,0xff103b85},
     {0xff071a2b,0xff071848,0xfff7efdc,0xff20172f,0xff0b2459},
     {0xff205773,0xff1883d8,0xffb99845,0xff705287,0xff2f9ad2},
     {0xff2aa3b8,0xff04c9ff,0xffeac367,0xffbd8649,0xff1aabff},
     {0xff0d668e,0xff0753c8,0xffca933a,0xff8b572d,0xff0a5fc7},
     {0xffd34257,0xfff43754,0xffb6251c,0xffbb243b,0xffe63349},
   };
   for(int[] p:pairs)if(p[0]==c)return p[Math.max(0,Math.min(theme,p.length-1))];
   if(theme==2 && (c==0xffffffff || c==0xff58d5e6))return 0xff253e23;
   return c;
 }
 void selectTheme(){new android.app.AlertDialog.Builder(this).setTitle("Choose controller design").setSingleChoiceItems(themes,theme,(dialog,which)->{
   theme=Math.max(0,Math.min(themes.length-1,which));getPreferences(0).edit().putInt("theme",theme).apply();int n=speed.getProgress();ui();speed.setProgress(n);if(adapter!=null && (Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED))loadDevices();if(output!=null)status.setText("●  Connected");dialog.dismiss();
 }).setNegativeButton("Cancel",null).show();}

 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 @Override public void onCreate(Bundle b){super.onCreate(b);adapter=BluetoothAdapter.getDefaultAdapter();theme=Math.max(0,Math.min(themes.length-1,getPreferences(0).getInt("theme",0)));float available=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density-90f;scale=Math.min(1f,Math.max(.60f,available/740f));ui();
   if(adapter==null){status.setText("Bluetooth unavailable");return;}
   if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
     requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},200);
   else loadDevices();
 }
 GradientDrawable bg(int a,int b,int radius,int stroke){
   GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{themeColor(a),themeColor(b)});
   g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(2),themeColor(stroke));return g;
 }
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 LinearLayout.LayoutParams m(int w,int h,int margin){
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h*scale));
   p.setMargins(dp(margin*scale),dp(margin*scale),dp(margin*scale),dp(margin*scale));return p;
 }
 LinearLayout.LayoutParams weight(int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(h*scale),1);p.setMargins(dp(2),dp(2),dp(2),dp(2));return p;}
 TextView label(String s,int size,int color,boolean bold){
   TextView t=new TextView(this);t.setText(s);t.setTextSize(size*scale);t.setTextColor(themeColor(color));
   t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
 }
 Button btn(String s,int a,int b,int txt){
   Button v=new Button(this);v.setAllCaps(false);v.setText(s);v.setTextColor(themeColor(txt));
   v.setTextSize(13*scale);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   v.setBackground(bg(a,b,17,GOLD));v.setElevation(dp(6));return v;
 }
 Button move(String name,String cmd,int color){
   Button v=btn(name,color,color==GREEN?0xff0b2339:color==BROWN?0xff10253d:0xff78162b,Color.WHITE);
   v.setOnTouchListener((view,event)->{
     if(event.getAction()==MotionEvent.ACTION_DOWN){v.setAlpha(.75f);send(cmd);return true;}
     if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){
       v.setAlpha(1f);if(!cmd.equals("S"))send("S");view.performClick();return true;
     }return true;
   });return v;
 }
 void ui(){
   LinearLayout root=col();root.setPadding(dp(7),dp(14),dp(7),dp(14));root.setBackgroundColor(themeColor(CREAM));root.setGravity(Gravity.CENTER_VERTICAL);
   setContentView(root);
   LinearLayout head=row();head.setPadding(dp(6),dp(3),dp(6),dp(3));head.setBackground(bg(0xff0b2236,0xff12314a,16,0xff215570));
   ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
   head.addView(logo,m(47,47,0));LinearLayout brand=col();brand.setPadding(dp(7),0,0,0);
   brand.addView(label("TARARBARI",17,Color.WHITE,true));brand.addView(label("BT Controler",15,GOLD,true));
   brand.addView(label("ENGINEERING IDEAS. BUILDING THE FUTURE.",9,0xffb8cba9,false));head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
   Button change=btn("THEMES",GREEN,0xff06364e,Color.WHITE);change.setTextSize(10*scale);change.setOnClickListener(v->selectTheme());head.addView(change,m(72,37,0));
   root.addView(head,m(-1,-2,0));

   LinearLayout connectCard=col();connectCard.setPadding(dp(6),dp(4),dp(6),dp(3));
   connectCard.setBackground(bg(0xff142d42,0xff0b1f30,13,0xff265675));
   LinearLayout statusRow=row();status=label("●  Disconnected",13,GOLD,true);statusRow.addView(status,new LinearLayout.LayoutParams(0,-2,1));
   Button connect=btn("CONNECT",GREEN,0xff06364e,Color.WHITE);connect.setOnClickListener(v->connect());
   statusRow.addView(connect,m(95,35,0));connectCard.addView(statusRow);
   devices=new Spinner(this);devices.setBackground(bg(0xffd9eaf0,0xffb3d2dc,9,0xff3095ac));
   connectCard.addView(devices,m(-1,30,0));root.addView(connectCard,m(-1,-2,4));
   LinearLayout speedCard=col();speedCard.setPadding(dp(7),dp(3),dp(7),dp(3));speedCard.setBackground(bg(0xff132d43,0xff102235,13,0xff26647e));
   speedLabel=label("◉  SPEED LEVEL  8",18,GOLD,true);speedCard.addView(speedLabel,m(-1,23,0));
   speed=new SeekBar(this);speed.setMax(9);speed.setProgress(8);
   speed.setProgressTintList(ColorStateList.valueOf(themeColor(GOLD)));speed.setThumbTintList(ColorStateList.valueOf(themeColor(GOLD)));
   speedCard.addView(speed,m(-1,26,0));

   speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
     public void onProgressChanged(SeekBar s,int p,boolean user){speedLabel.setText("◉  SPEED LEVEL  "+p);if(user)send(""+p);}
     public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
   });root.addView(speedCard,m(-1,-2,4));
   root.addView(label("◈  MAIN DRIVE CONSOLE",14,GOLD,true),m(-1,21,1));
   LinearLayout pad=col();pad.setPadding(dp(2),dp(3),dp(2),dp(3));pad.setBackground(bg(0xff102c42,0xff071a2b,14,0xff205773));
   LinearLayout up=row();up.addView(new View(this),weight(49));up.addView(move("▲\nFORWARD","F",GREEN),weight(65));up.addView(new View(this),weight(65));pad.addView(up);
   LinearLayout center=row();center.addView(move("◀\nLEFT","L",GREEN),weight(53));center.addView(move("■\nSTOP","S",RED),weight(74));center.addView(move("▶\nRIGHT","R",GREEN),weight(74));pad.addView(center);
   LinearLayout down=row();down.addView(new View(this),weight(65));down.addView(move("▼\nBACKWARD","B",GREEN),weight(65));down.addView(new View(this),weight(65));pad.addView(down);
   root.addView(pad,m(-1,-2,3));
   root.addView(label("◈  PRECISION SHORTCUTS",14,GOLD,true),m(-1,30,2));
   LinearLayout diag1=row();diag1.addView(move("↖ FORWARD-LEFT","G",BROWN),weight(43));diag1.addView(move("↗ FORWARD-RIGHT","I",BROWN),weight(56));root.addView(diag1);
   LinearLayout diag2=row();diag2.addView(move("↙ BACKWARD-LEFT","H",BROWN),weight(56));diag2.addView(move("↘ BACKWARD-RIGHT","J",BROWN),weight(56));root.addView(diag2);
   Button max=btn("⚡  MAX SPEED (Q)",0xff2aa3b8,0xff0d668e,Color.WHITE);max.setOnClickListener(v->{send("q");speed.setProgress(9);});
   root.addView(max,m(-1,41,2));
   Button disconnect=btn("⛓  DISCONNECT",0xffb52a45,RED,Color.WHITE);disconnect.setOnClickListener(v->disconnect());
   root.addView(disconnect,m(-1,54,4));
 }
 void loadDevices(){
   try{
     paired.clear();ArrayList<String> names=new ArrayList<>();
     for(BluetoothDevice d:adapter.getBondedDevices()){paired.add(d);names.add((d.getName()==null?"Bluetooth device":d.getName())+" ("+d.getAddress()+")");}
     if(names.isEmpty())names.add("Pair HC-05 / HC-06 in Settings");
     devices.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
   }catch(SecurityException e){status.setText("Allow Nearby Devices permission");}
 }
 void connect(){
   if(adapter==null)return;
   if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
     requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},200);return;
   }
   int i=devices.getSelectedItemPosition();if(i<0||i>=paired.size()){status.setText("Pair HC-05 / HC-06 first");return;}
   BluetoothDevice d=paired.get(i);status.setText("●  Connecting...");
   new Thread(()->{
     BluetoothSocket s=null;
     try{s=d.createRfcommSocketToServiceRecord(SPP);adapter.cancelDiscovery();s.connect();
       BluetoothSocket connected=s;OutputStream stream=s.getOutputStream();
       runOnUiThread(()->{socket=connected;output=stream;status.setText("●  Connected: "+d.getName());});
     }catch(Exception e){try{if(s!=null)s.close();}catch(Exception ignored){}
       runOnUiThread(()->status.setText("Connection failed. Check pairing/power."));}
   }).start();
 }
 void send(String s){
   try{if(output==null){status.setText("Not connected — connect first");return;}
     output.write(s.getBytes("US-ASCII"));output.flush();
   }catch(Exception e){status.setText("Bluetooth send failed");}
 }
 void disconnect(){
   if(output!=null)send("S");
   try{if(socket!=null)socket.close();}catch(Exception ignored){}
   socket=null;output=null;status.setText("●  Disconnected");
 }
 @Override public void onRequestPermissionsResult(int code,String[] perms,int[] results){
   super.onRequestPermissionsResult(code,perms,results);
   if(code==200){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)loadDevices();else status.setText("Bluetooth permission required");}
 }
 @Override public void onDestroy(){try{if(socket!=null)socket.close();}catch(Exception ignored){}super.onDestroy();}
}