package uk.v2g.ocpp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractScheduledService;

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.core.AuthorizationStatus;
import eu.chargetime.ocpp.model.core.AuthorizeConfirmation;
import eu.chargetime.ocpp.model.core.IdTagInfo;
import eu.chargetime.ocpp.test.FakeChargePoint;
import eu.chargetime.ocpp.model.core.StartTransactionConfirmation;

public class V2GChargeSession implements Runnable
{
	private static final Logger logger = LoggerFactory.getLogger(V2GChargeSession.class);
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH);
	
	private FakeChargePoint chargePoint = null;
	private String userID = null;
	private int connectorID = 41;
	private int idTransaction = -1;
	private int currentMeter = 42;
	
	private static final int MAX_SLEEPS = 30;		// 5 minutes
	private static final int SLEEP_TIME = 10000;	// 10 seconds
	
	public V2GChargeSession(FakeChargePoint chargePoint, String ID)
	{
		this.chargePoint = chargePoint;
		this.userID = ID;
	}
	
	public void run()
	{
		try {
	        BiConsumer<Confirmation, Throwable> startCallback = new BiConsumer<Confirmation, Throwable>() {
	            @Override
	            public void accept(Confirmation result, Throwable t) {
	            	StartTransactionConfirmation stc =(StartTransactionConfirmation) result;
	            	idTransaction = stc.getTransactionId();
	            	IdTagInfo idInfo = stc.getIdTagInfo();	            	
	                logger.info("Start transaction confirmed - tID " + idTransaction + ", pID " + idInfo.getParentIdTag() + ", Status " + idInfo.getStatus() + ", Expires "  + dateFormat.format(idInfo.getExpiryDate().getTime()));

	                TransactionWait transactionWait = new TransactionWait();
	                transactionWait.startAsync();
	                return;
	            }
	        };
	        
		    BiConsumer<Confirmation, Throwable> authCallback = new BiConsumer<Confirmation, Throwable>() {
	            @Override
	            public void accept(Confirmation result, Throwable t) {
	            	AuthorizeConfirmation ac = (AuthorizeConfirmation) result;
	            	IdTagInfo idInfo = ac.getIdTagInfo();
	            	AuthorizationStatus status = idInfo.getStatus();
	                logger.info("Authorize confirmed - Parent ID " + idInfo.getParentIdTag() + ", Status " + status + ", Expires "  + dateFormat.format(idInfo.getExpiryDate().getTime()));
                	try {
    	                if (status == AuthorizationStatus.Accepted)
    	                	chargePoint.sendStartTransactionRequest(connectorID, userID, currentMeter, startCallback);
					} catch (Exception ex) {
		                logger.error("Start transaction error - ", ex);
	                }
	            }
	        };
	        
	        chargePoint.sendAuthorizeRequest(userID, authCallback);
		}
		catch (Exception ex) {
            logger.error("Authorize error - ", ex);
		}
	}
	
    // Inner classes
    //
    protected class TransactionWait extends AbstractScheduledService {
        private int sleepCount;

        protected void startUp() throws Exception {
            sleepCount = MAX_SLEEPS;
        }

        protected void runOneIteration() throws Exception {
            if (chargePoint.hasReceivedStopTransactionConfirmation()) {
                chargePoint.disconnect();
                stopAsync();
            }
            else if (chargePoint.hasHandledRemoteStopTransactionRequest()) {
                chargePoint.disconnect();
                stopAsync();
            }
            else if (sleepCount-- < 0) {
            	logger.error("Charging session timed out - Terminating");
            	chargePoint.sendStopTransactionRequest(currentMeter, idTransaction);
            }
            else {
            	currentMeter++;
            }
            	
        }

        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(SLEEP_TIME, SLEEP_TIME, TimeUnit.MILLISECONDS);
        }
    }

}
