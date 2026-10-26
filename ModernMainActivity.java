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
 static final int GREEN=0xff244b24, GOLD=0xffa3ff4f, CREAM=0xff07120d, BROWN=0xff244126, RED=0xffb41418;
 static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 BluetoothAdapter adapter; BluetoothSocket socket; OutputStream output;
 TextView status, speedLabel; Spinner devices; SeekBar speed;
 ArrayList<BluetoothDevice> paired=new ArrayList<>();
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 @Override public void onCreate(Bundle b){super.onCreate(b);adapter=BluetoothAdapter.getDefaultAdapter();ui();
   if(adapter==null){status.setText("Bluetooth unavailable");return;}
   if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
     requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},200);
   else loadDevices();
 }
 GradientDrawable bg(int a,int b,int radius,int stroke){
   GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});
   g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(2),stroke);return g;
 }
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 LinearLayout.LayoutParams m(int w,int h,int margin){
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));
   p.setMargins(dp(margin),dp(margin),dp(margin),dp(margin));return p;
 }
 LinearLayout.LayoutParams weight(int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(h),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));return p;}
 TextView label(String s,int size,int color,boolean bold){
   TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
   t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
 }
 Button btn(String s,int a,int b,int txt){
   Button v=new Button(this);v.setAllCaps(false);v.setText(s);v.setTextColor(txt);
   v.setTextSize(14);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   v.setBackground(bg(a,b,17,GOLD));v.setElevation(dp(6));return v;
 }
 Button move(String name,String cmd,int color){
   Button v=btn(name,color,color==GREEN?0xff081d0e:color==BROWN?0xff0b2416:0xff650b12,Color.WHITE);
   v.setOnTouchListener((view,event)->{
     if(event.getAction()==MotionEvent.ACTION_DOWN){v.setAlpha(.75f);send(cmd);return true;}
     if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){
       v.setAlpha(1f);if(!cmd.equals("S"))send("S");view.performClick();return true;
     }return true;
   });return v;
 }
 void ui(){
   ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(CREAM);
   LinearLayout root=col();root.setPadding(dp(11),dp(8),dp(11),dp(16));root.setBackgroundColor(CREAM);
   scroll.addView(root);setContentView(scroll);
   LinearLayout head=row();head.setPadding(dp(10),dp(8),dp(10),dp(8));head.setBackground(bg(0xff0b1815,0xff193c24,20,GOLD));
   ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
   head.addView(logo,m(74,74,0));LinearLayout brand=col();brand.setPadding(dp(10),0,0,0);
   brand.addView(label("TARARBARI",21,Color.WHITE,true));brand.addView(label("BT Controler",18,GOLD,true));
   brand.addView(label("ENGINEERING IDEAS. BUILDING THE FUTURE.",9,0xffb8cba9,false));head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
   root.addView(head,m(-1,-2,0));
   TextView sub=label("●  ROVER CONTROL SYSTEM     •     BLUETOOTH SPP",11,GOLD,true);sub.setPadding(dp(9),dp(9),0,dp(4));root.addView(sub);
   LinearLayout connectCard=col();connectCard.setPadding(dp(12),dp(9),dp(12),dp(9));
   connectCard.setBackground(bg(0xff11221a,0xff0a1512,18,0xff385e35));
   LinearLayout statusRow=row();status=label("●  Disconnected",15,GREEN,true);statusRow.addView(status,new LinearLayout.LayoutParams(0,-2,1));
   Button connect=btn("CONNECT",GREEN,0xff172c17,Color.WHITE);connect.setOnClickListener(v->connect());
   statusRow.addView(connect,m(110,46,0));connectCard.addView(statusRow);
   devices=new Spinner(this);devices.setBackground(bg(0xffdfeecf,0xffbfcda9,10,GOLD));
   connectCard.addView(devices,m(-1,42,0));root.addView(connectCard,m(-1,-2,4));
   LinearLayout speedCard=col();speedCard.setPadding(dp(12),dp(8),dp(12),dp(8));speedCard.setBackground(bg(Color.WHITE,0xfff3eddb,18,GOLD));
   speedLabel=label("◉  SPEED LEVEL  8",18,GOLD,true);speedCard.addView(speedLabel,m(-1,31,0));
   speed=new SeekBar(this);speed.setMax(9);speed.setProgress(8);
   speed.setProgressTintList(ColorStateList.valueOf(GOLD));speed.setThumbTintList(ColorStateList.valueOf(GOLD));
   speedCard.addView(speed,m(-1,41,0));
   speedCard.addView(label("0                                            9  /  MAX",11,GOLD,false));
   speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
     public void onProgressChanged(SeekBar s,int p,boolean user){speedLabel.setText("◉  SPEED LEVEL  "+p);if(user)send(""+p);}
     public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
   });root.addView(speedCard,m(-1,-2,4));
   root.addView(label("◈  MAIN DRIVE CONSOLE",14,GOLD,true),m(-1,30,2));
   LinearLayout pad=col();pad.setPadding(dp(2),dp(3),dp(2),dp(3));pad.setBackground(bg(0xff0b2119,0xff09120f,19,0xff42632d));
   LinearLayout up=row();up.addView(new View(this),weight(65));up.addView(move("▲\nFORWARD","F",GREEN),weight(65));up.addView(new View(this),weight(65));pad.addView(up);
   LinearLayout center=row();center.addView(move("◀\nLEFT","L",GREEN),weight(74));center.addView(move("■\nSTOP","S",RED),weight(74));center.addView(move("▶\nRIGHT","R",GREEN),weight(74));pad.addView(center);
   LinearLayout down=row();down.addView(new View(this),weight(65));down.addView(move("▼\nBACKWARD","B",GREEN),weight(65));down.addView(new View(this),weight(65));pad.addView(down);
   root.addView(pad,m(-1,-2,3));
   root.addView(label("◈  PRECISION SHORTCUTS",14,GOLD,true),m(-1,30,2));
   LinearLayout diag1=row();diag1.addView(move("↖ FORWARD-LEFT","G",BROWN),weight(56));diag1.addView(move("↗ FORWARD-RIGHT","I",BROWN),weight(56));root.addView(diag1);
   LinearLayout diag2=row();diag2.addView(move("↙ BACKWARD-LEFT","H",BROWN),weight(56));diag2.addView(move("↘ BACKWARD-RIGHT","J",BROWN),weight(56));root.addView(diag2);
   Button max=btn("⚡  MAX SPEED (Q)",0xff9be34d,GOLD,0xff071509);max.setOnClickListener(v->{send("q");speed.setProgress(9);});
   root.addView(max,m(-1,54,4));
   Button disconnect=btn("⛓  DISCONNECT",0xffb52b21,RED,Color.WHITE);disconnect.setOnClickListener(v->disconnect());
   root.addView(disconnect,m(-1,54,4));
   TextView note=label("ⓘ Pair HC-05 / HC-06 in Bluetooth settings first.\nPIN: 1234 or 0000. Release a driving button to stop.",12,GREEN,false);
   note.setPadding(dp(12),dp(10),dp(12),dp(10));note.setBackground(bg(0xff17291c,0xff0b1812,16,0xff42632d));
   root.addView(note,m(-1,-2,4));
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