package eu.innovationengineering.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("confidence")
@Produces(value = { MediaType.APPLICATION_JSON })
@Consumes(value = { MediaType.APPLICATION_JSON })
public interface ConfidenceRequester {
  
  @POST
  @Path("/get")
  ConfidenceResponse computeConfidence(ConfidenceRequest request) throws Exception;
  
}
