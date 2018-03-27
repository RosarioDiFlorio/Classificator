package eu.innovationengineer.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("keywords")
@Produces(value = { MediaType.APPLICATION_JSON })
@Consumes(value = { MediaType.APPLICATION_JSON })
public interface KeywordRequester {
  
  @POST
  @Path("/get")
  KeywordResponse computeConfidence(KeywordRequest request) throws Exception;
  
}
