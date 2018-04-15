package uk.v2g.ocpp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import eu.chargetime.ocpp.test.FakeChargePoint;
import eu.chargetime.ocpp.test.FakeChargePoint.clientType;

public class V2GChargePoint {
	private final String CP_VENDOR = "V2G Limited";
	private final String CP_MODEL = "EVSE Alpha";
	private final String EV_ID = "AB12CDE";
    
	private static final Logger logger = LoggerFactory.getLogger(FakeChargePoint.class);
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH);
	
	private FakeChargePoint client; 

	public V2GChargePoint(clientType type, String host, int port)
	{
		this(type, host, port, null);
	}
	
	public V2GChargePoint(clientType type, String host, int port, String path) {
		try {
			client = new FakeChargePoint(type, host, port, path);
		}
		catch (Exception ex) {
        	logger.error(this.getClass().getEnclosingMethod().getName(), ex);
		}
	}
	
	public void run()
	{
		try {
			BiConsumer<Confirmation, Throwable> bootCallback = new BiConsumer<Confirmation, Throwable>() {
	            @Override
	            public void accept(Confirmation result, Throwable t) {
	            	BootNotificationConfirmation bnc = (BootNotificationConfirmation) result;
	                logger.info("V2GChargePoint boot confirmed - Status " + bnc.getStatus() + " at " + dateFormat.format(bnc.getCurrentTime().getTime()));
	                // For demo purposes start a charging session immediately
	                V2GChargeSession session = new V2GChargeSession(client, EV_ID);
	                Thread sessionThread = new Thread(session);
	                sessionThread.start();
	            }
	        };
			client.connect(CP_VENDOR, CP_MODEL, bootCallback);
		}
		catch (Exception ex) {
        	logger.error(this.getClass().getEnclosingMethod().getName(), ex);
		}
	}
}
