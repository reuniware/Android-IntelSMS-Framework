/*
 * Created by SharpDevelop.
 * User: Idjed
 * Date: 20/11/2015
 * Time: 09:56
 * 
 * To change this template use Tools | Options | Coding | Edit Standard Headers.
 */
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using IntelSmsServer.DataModel;

namespace IntelSmsServer
{
	/// <summary>
	/// Description of MainForm.
	/// </summary>
	public partial class MainForm : Form
	{
		const String INITIAL_IP_ADDRESS = "192.168.0.13";
		
		void MainFormLoad(object sender, EventArgs e)
		{
			txtIpAddress.Text = INITIAL_IP_ADDRESS;	
		}

		public MainForm()
		{
			InitializeComponent();			
		}
		 
		TcpClient clientSocket = null;
		NetworkStream networkStream = null;
		StreamReader reader = null;
		StreamWriter writer = null;
		
		public List<AndroidSms> getListOfSms(String androidIpAddress)
		{
			List<AndroidSms> lstAndroidSms = new List<AndroidSms>();
			try
			{
				if (reader != null)
					reader.Close();
				
				if (writer != null)
					writer.Close();
				
				if (networkStream != null)
					networkStream.Close();

				if (clientSocket != null)
					clientSocket.Close();
								
				clientSocket = new TcpClient();
				clientSocket.SendTimeout = 20000;
				clientSocket.ReceiveTimeout = 20000;
				clientSocket.Connect(androidIpAddress, 7777);
				
				networkStream = clientSocket.GetStream();

				reader = new StreamReader(networkStream);
				writer = new StreamWriter(networkStream);
				
				string toserver = "get sms";
				writer.AutoFlush = true;
				writer.Write(toserver);
				log("sent to server:" + toserver);
				writer.Flush();
				
				string fromserver = reader.ReadToEnd();
				//log("len from server:" + fromserver.Length);
				//log("received from server:" + fromserver);
				
				//networkStream.Close();
				//clientSocket.Close();
				
				if (fromserver.Contains(" _id:")) {
					string[] sms = fromserver.Split(new String[] { " _id:" }, StringSplitOptions.None);
					//log("sms count = " + sms.GetLength(0));
					for (int i = 0; i < sms.GetLength(0); i++) {
						//log("Processing SMS#" + i);
						string[] smsdata = sms[i].Split(new String[] { " " }, StringSplitOptions.None);
						//log("smsdata len = " + smsdata.GetLength(0));
						if (smsdata.GetLength(0) > 34) {
							//log("---> smsdata#" + j + " : " + smsdata[j]);
							lstAndroidSms.Add(new AndroidSms(smsdata));
						}
					}
				}
				
				log("Number of SMS retrieved from Android = " + lstAndroidSms.Count);
				Int32 indexSms = 0;
				foreach (AndroidSms androidSms in lstAndroidSms) {
					log("========SMS#" + (indexSms++) + "========");
					log("_id=" + androidSms._id);
					log("address=" + androidSms.address);
					log("date=" + androidSms.date);
					log("date_sent=" + androidSms.date_sent);
					log("delivery_date=" + androidSms.delivery_date);
					log("body=" + androidSms.body);
					log("service_center=" + androidSms.service_center);
					log("type=" + androidSms.type); // type=1<=>inbox type=2<=>sent
				}
				
			} catch (Exception ex) {
				log("001:" + ex.Message);
			}
			return lstAndroidSms;
		}

		String getListOfContacts(String androidIpAddress)
		{
			try
			{
				if (reader != null)
					reader.Close();
				
				if (writer != null)
					writer.Close();
				
				if (networkStream != null)
					networkStream.Close();

				if (clientSocket != null)
					clientSocket.Close();
								
				clientSocket = new TcpClient();
				clientSocket.SendTimeout = 10000;
				clientSocket.ReceiveTimeout = 10000;
				clientSocket.Connect(androidIpAddress, 7777);
				
				networkStream = clientSocket.GetStream();

				reader = new StreamReader(networkStream);
				writer = new StreamWriter(networkStream);
				
				string toserver = "get contacts";
				writer.AutoFlush = true;
				writer.Write(toserver);
				log("sent to server:" + toserver);
				writer.Flush();
				
				string fromserver = reader.ReadToEnd();
				//log("len from server:" + fromserver.Length);
				log("received from server:" + fromserver);
								
			} catch (Exception ex) {
				log("001:" + ex.Message);
			}
			
			return "";
		}
		
		
		void BtnConnectClick(object sender, EventArgs e)
		{
			String androidIpAddress = txtIpAddress.Text;
			List<AndroidSms> lstAndroidSms = getListOfSms(androidIpAddress);
			log("NB SMS = " + lstAndroidSms.Count);
		}
		
		void BtnGetContactsClick(object sender, EventArgs e)
		{
			String androidIpAddress = txtIpAddress.Text;
		
			String contacts = getListOfContacts(androidIpAddress);
			log("RESPONSE = " + contacts);	
		}
		
		private void log(string str)
		{
			this.txtLog.AppendText(str + "\r\n");
		}
		
		void BtnPingClick(object sender, EventArgs e)
		{
			String androidIpAddress = txtIpAddress.Text;
			Ping pingSender = new Ping();
			PingReply pingReply = pingSender.Send(androidIpAddress, 1000);
			//log(pingReply.Status.ToString());
			if (pingReply.Status != IPStatus.Success)
			{
				log("IP address not reachable (" + pingReply.Status.ToString() + ").");
			}
			else if (pingReply.Status == IPStatus.Success)
			{
				log("IP address is reachable.");
				TcpClient s = null;
				
				try{
					if (s!=null) s.Connect(androidIpAddress, 7777);
					if (s!=null) s.Close();
					log("Connection attempt OK.");
				}catch(Exception ex)
				{
					log("Connection attempt ERROR (" + ex.Message + ").");
					if (s != null) s.Close();					
				}

			}
		}
				
	}
}
