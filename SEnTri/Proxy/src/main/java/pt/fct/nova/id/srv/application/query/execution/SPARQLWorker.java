package pt.fct.nova.id.srv.application.query.execution;


import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface SPARQLWorker {
    IRITable exec(Job job) throws SPARQLExecutionException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException;

    IRITable exec(Job1 job, IRITable prevJobResults) throws SPARQLExecutionException;

    IRITable exec(Job2 job, IRITable left, IRITable right) throws SPARQLExecutionException;

    SPARQLResult generateResults(IRITable jobResults);

}
