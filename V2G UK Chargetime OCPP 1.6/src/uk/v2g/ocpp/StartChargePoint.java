package uk.v2g.ocpp;

import eu.chargetime.ocpp.test.FakeChargePoint.clientType;

public class StartChargePoint {
	public static void main(String[] args) {
		try {
			String host = "localhost";
			clientType type = clientType.JSON;
			int port = 8080;
//			int port = 8887;
			String path = "steve/websocket/CentralSystemService";
//			String path = null;
			
			if (args.length > 0)
				host = args[0];
			if (args.length > 1)
				port = Integer.parseInt(args[1]);
			if (args.length > 2)
				type = clientType.valueOf(args[2]);
			if (args.length > 3)
				path = args[3];
			
//			V2GChargePoint cp = new V2GChargePoint(type, host, 8887, null);
			V2GChargePoint cp = new V2GChargePoint(type, host, port, path);
			cp.run();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
