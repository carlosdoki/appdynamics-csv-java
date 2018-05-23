package com.appdynamics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ExtractCSV {

	public JSONArray chamadasURL(String urlEndpoint, String usuario, String senha, String account) {
		URL url;
		JSONArray a;

		try {
			url = new URL(urlEndpoint);
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(30000); // 30 seconds time out
			Base64.Encoder enc= Base64.getEncoder();
			String encoded = "";

			if (usuario != null && senha != null && account != null){
				String user_pass = usuario + "@" + account + ":" + senha;

				encoded = enc.encodeToString( user_pass.getBytes() );
				conn.setRequestProperty("Authorization", "Basic " + encoded);
			}

			String line = "";
			StringBuffer sb = new StringBuffer();
			BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()) );
			while((line = input.readLine()) != null)
				sb.append(line);
			input.close();
			JSONParser parser = new JSONParser();
			a = (JSONArray) parser.parse(sb.toString());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	
			return null;	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return a;
	}

	public ExtractCSV(String controller, String porta, String usuario, String senha, String account, String intervalo) {
		//Create connection

		String endpoint = controller + ":" + porta + "/controller/rest/applications?output=json";
		URL url;
		String cabecalho = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ";" + intervalo;

		//		###TS;DURATION;SYSNM;OBJNM;SUBOBJNM;VALUE;DS_SYSNM
		//		#2008-05-22 23:59:00;60;192.168.100.41;MEM_FREE;GLOBAL;2.22E7;192.168.100.41

		JSONArray aApplications = chamadasURL(endpoint, usuario, senha, account);

		for (Object oapplication : aApplications) {
			JSONObject application = (JSONObject) oapplication;

			System.out.println(application.get("name"));
			endpoint = controller + ":" + porta + "/controller/rest/applications/" + application.get("name").toString().replace(" ", "%20") + "/business-transactions?output=json";
			JSONArray aBTs = chamadasURL(endpoint, usuario, senha, account);

			try {
				FileOutputStream outputStream = new FileOutputStream(application.get("name").toString() + ".csv");
				PrintWriter writer = new PrintWriter(outputStream);
				for (Object oBTs : aBTs) {
					JSONObject BT = (JSONObject) oBTs;
					if (! BT.get("internalName").toString().contains("_APPDYNAMICS_DEFAULT_TX_")) {
						try {
							endpoint = controller + ":" + porta + "/controller/rest/applications/" + URLEncoder.encode(application.get("name").toString(), "UTF-8") + "/metric-data?metric-path=Business%20Transaction%20Performance%7CBusiness%20Transactions%7C" + URLEncoder.encode(BT.get("tierName").toString(), "UTF-8")  + "%7C" + URLEncoder.encode(BT.get("internalName").toString(), "UTF-8")  + "%7CAverage%20Response%20Time%20%28ms%29&time-range-type=BEFORE_NOW&duration-in-mins=" + intervalo + "&output=json";
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						JSONArray aAverage = chamadasURL(endpoint, usuario, senha, account);
						for (Object oAverage : aAverage) {
							JSONObject average = (JSONObject) oAverage;
							JSONArray aValues = (JSONArray) average.get("metricValues");
							for (Object oValues : aValues) {
								JSONObject value = (JSONObject) oValues;
								String texto = cabecalho+";"+application.get("name").toString()+";"+BT.get("internalName").toString()+";Average Response Time total;" + value.get("value").toString() ; 
								writer.println(texto);
//								outputStream.write(texto.getBytes());
//								outputStream.
								//System.out.println(value.get("value"));
							}
						}

						//Business Transaction Performance|Business Transactions|Estacoes|Login|Calls per Minute
						try {
							endpoint = "Business Transaction Performance|Business Transactions|"+BT.get("tierName").toString()+"|"+BT.get("internalName").toString()+"|Calls per Minute";
							endpoint = URLEncoder.encode(endpoint, "UTF-8");
							endpoint = controller + ":" + porta + "/controller/rest/applications/" + URLEncoder.encode(application.get("name").toString(), "UTF-8") + "/metric-data?metric-path=" + endpoint + "&time-range-type=BEFORE_NOW&duration-in-mins=" + intervalo + "&output=json";
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						JSONArray aCalls = chamadasURL(endpoint, usuario, senha, account);
						for (Object oCalls : aCalls) {
							JSONObject call = (JSONObject) oCalls;
							JSONArray aValues = (JSONArray) call.get("metricValues");
							for (Object oValues : aValues) {
								JSONObject value = (JSONObject) oValues;
								String texto = cabecalho+";"+application.get("name").toString()+";"+BT.get("internalName").toString()+";Call per Minute por minuto;" + value.get("value").toString(); 
//								outputStream.write(texto.getBytes());
//								System.out.println(value.get("value"));
								writer.println(texto);
								texto = cabecalho+";"+application.get("name").toString()+";"+BT.get("internalName").toString()+";Call per minute periodo;" + value.get("sum").toString(); 
//								outputStream.write(texto.getBytes());
//								System.out.println(value.get("sum"));
								writer.println(texto);
							}
						}
					}
				}
				outputStream.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return;
	}

	public static void main(String[] args) {
		if (args.length < 5) {
			System.out.println("Numero de parametros invalido!\n");
			System.out.println("Parametros: <http://controller> <porta> <usuario> <senha> <sccount> <intervalo>\n");
			return;		
		}
		String data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); 
		System.out.println(data);
		System.out.println("=================================================");
		new ExtractCSV (args[0], args[1], args[2], args[3], args[4], args[5]);
		data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		System.out.println("=================================================");
		System.out.println(data);
	}

}
