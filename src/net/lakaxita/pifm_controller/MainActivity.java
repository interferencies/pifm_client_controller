package net.lakaxita.pifm_controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;


public class MainActivity extends Activity {

	private Button bt1,bt2,bt3,bt4,bt5;
	private EditText dial;
	private SeekBar slice;
	private TextView tv;
	private Socket socket;
	private String serverIpAddress = "192.168.0.101";
	private String freq;
	private static final int SERVERPORT = 8001;
	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	private Thread cThread; 
	private String server_response;
	
	public String get_response() {
		return this.server_response;
	}
	
	public void set_response(String r) {
		this.server_response = r;
	}
	
	public float get_freq(int progress) {
		return (float) (progress / 10.0) + 87;
	}
	
	protected static final int CONECTADO = 0x100;
	protected static final int DESCONECTADO = 0x101;
	protected static final int RESPONSE = 0x102;
	protected static final int ERROR_CONEXION = 0x103;

	
	Handler vistaHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MainActivity.CONECTADO:
                // Invalidar vista para repintado
            	tv.setText("Conectado");
            	bt5.setText("Desconectar");
                break;
            case MainActivity.DESCONECTADO:
                // Invalidar vista para repintado
            	tv.setText("Desconectado");
            	bt5.setText("Conectar");
                break;
            case MainActivity.ERROR_CONEXION:
                // Invalidar vista para repintado
            	tv.setText("Error de conexión");
                break;
            case MainActivity.RESPONSE:
            	tv.setText(get_response());
            	break;
            }
            super.handleMessage(msg);
        }
    };
	
	@Override
	protected void onStop() {
		super.onStop();
		try {
			if (socket!=null && socket.isConnected()) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		StrictMode.setThreadPolicy(policy);
		
		setContentView(R.layout.activity_main);
		bt1 = (Button) findViewById(R.id.button1);//start
		bt2 = (Button) findViewById(R.id.button2);//stop
		bt3 = (Button) findViewById(R.id.button3);//info
		bt4 = (Button) findViewById(R.id.button4);//change freq
		bt5 = (Button) findViewById(R.id.btn_connect);//conecta
		dial = (EditText) findViewById(R.id.editText1); //visualizador de freq
		slice = (SeekBar) findViewById(R.id.seekBar1);
		tv = (TextView) findViewById(R.id.textView2);
		
		dial.setText(String.valueOf(get_freq(slice.getProgress())));
		Log.d("Client","Slice: "+slice.getProgress());
		
        bt1.setOnClickListener(connectListener);
        bt2.setOnClickListener(connectListener);
        bt3.setOnClickListener(connectListener);
        bt4.setOnClickListener(connectListener);
        
        slice.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// progress max 210
				//210 => 180.0
				dial.setText(String.valueOf(get_freq(progress)));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        
        bt5.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				cThread = new Thread(new ClientThread());
				cThread.start(); 
			}
        });
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		EditText ip = (EditText) findViewById(R.id.editText2);
		serverIpAddress = ip.getText().toString();
	}
	
	private OnClickListener connectListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        	tv = (TextView) findViewById(R.id.textView2);
    		try {
    			if (socket!=null && socket.isConnected() ) {
		            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
		            String msg = "";
		            tv.setText("Esperando respuesta..");
		            switch (v.getId()) {
		            case R.id.button1: msg="{\"command\":\"start\"}"; break;
		            case R.id.button2: msg="{\"command\":\"stop\"}"; break;
		            case R.id.button3: msg="{\"command\":\"info\"}"; break;
		            case R.id.button4: msg="{\"command\":\"sintonize\",\"freq\":\""+dial.getText().toString()+"\"}"; break;
		            }
		            out.println(msg); // envio del json de start al socket

	            } else {tv.setText("No se puede conectar al socket");}
    		} catch (IOException io) {io.printStackTrace();}
			catch (NullPointerException np) {np.printStackTrace();}
        }
    };
	
    public class ReadThread implements Runnable {
		@Override
		public void run() {
			Log.d("Client","Read Thread");
            BufferedReader input;
			try {
				while (socket!=null && socket.isConnected()) {
					input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					Log.d("Client","Waiting Response");
					set_response(input.readLine());
					Message msg = new Message();
					msg.what = MainActivity.RESPONSE;
					MainActivity.this.vistaHandler.sendMessage(msg);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }
    
	public class ClientThread implements Runnable {
		 
		public void kill() throws IOException {
			if (socket!=null && socket.isConnected()) { 
				socket.close(); 
			}
            Log.d("ClientActivity", "C: Closed.");
		}
		
        public void run() {
        	Message message = new Message();
            try {
                if (socket!=null && socket.isConnected()) {
                	socket.close();
                	socket=null;
	                Log.d("ClientActivity", "C: Desconectando...");
                	message.what = MainActivity.DESCONECTADO;
                } else {
	            	InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
	                Log.d("ClientActivity", "C: Connecting...");
	                socket = new Socket(serverAddr, MainActivity.SERVERPORT);
	                message.what = MainActivity.CONECTADO;
	                Thread CRead = new Thread(new ReadThread());
					CRead.start();
                }
                MainActivity.this.vistaHandler.sendMessage(message);
            } catch (Exception e) {
            	message.what=MainActivity.ERROR_CONEXION;
            	MainActivity.this.vistaHandler.sendMessage(message);
                Log.e("ClientActivity", "C: Error", e);
            }
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
