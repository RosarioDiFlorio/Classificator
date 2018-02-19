package eu.innovation.engineering.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/wikipedia-graph")
@Produces(value = { MediaType.APPLICATION_JSON })
@Consumes(value = { MediaType.APPLICATION_JSON })
public interface WikiGraphRequest {

  @POST
  @Path("/build")
  GraphResponse buildGraph(GraphRequest request);
  
  
}
