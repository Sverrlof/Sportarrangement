/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.sport.service;


import com.mycompany.sport.auth.AuthenticationService;
import com.mycompany.sport.auth.Group;
import com.mycompany.sport.auth.User;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import net.coobird.thumbnailator.Thumbnails;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;


/**
 *
 * @author sigurd
 */

@Path("event")
@Stateless
@DeclareRoles({Group.USER})
public class SportService {
    
    @Inject
    AuthenticationService authService;
        
    @Context
    SecurityContext sc;
        
    @PersistenceContext
    EntityManager em;
    
   // @Inject
   // MailService mailService;
    
    /**
     * Lists all events
     * @return a list of all events
     */
    
    @GET
    @Path("allevents")
    public List<Event> getAllEvents() {
        return em.createNamedQuery(Event.FIND_ALL_EVENTS, Event.class).getResultList();
    }

    
    /**
     * A path where new events can be added and stored in the database
     * 
     * @param sport
     * @param date
     * @param description
     * @param location
     * @param time
     * @param maxPlayers
     * @param latLng
     * @return 
     */
    
    @POST
    @Path("add")
    @RolesAllowed({Group.USER})
    public Response addEvent(
                @FormParam("sport") String sport,
                @FormParam("description") String description,
                @FormParam("date") String date,
                @FormParam("location") String location,
                @FormParam("time") String time,
                @FormParam("maxPlayers") int maxPlayers,
                @FormParam("latLng") String latLng){
        
        User user = this.getCurrentUser();
        Event newEvent = new Event();
      
        newEvent.setEventCreator(user);
        newEvent.setSport(sport);
        newEvent.setDescription(description);
        newEvent.setDate(date);
        newEvent.setLocation(location);
        newEvent.setTime(time);
        newEvent.setMaxPlayers(maxPlayers);
        newEvent.setLatLng(latLng);
        
        em.persist(newEvent);
        return Response.ok().build();
    }
   
    @DELETE
    @Path("remove")
    @RolesAllowed({Group.USER})
    public Response delete(@QueryParam("eventid")Long eventid){
        Event event = em.find(Event.class, eventid);
        if(event !=null){
            User user = this.getCurrentUser();
            if(event.getEventCreator().getUserid().equals(user.getUserid()))
                em.remove(event);
            return Response.ok().build();
        }
        return Response.notModified().build();
    }
    
    @GET
    @Path("eventattenders")
    @RolesAllowed({Group.USER})
    public List<User> getAttenders(@QueryParam("eventid") Long eventid){
        Event event = em.find(Event.class, eventid);
                if(event != null) {
           return event.getEventAttenders();
        }
                //This will return a list of all users if the event = null:
                //It's impossible to get to that call if you're not in an event
                //@TODO Check if we can just return an empty list instead, but its currently working
                return em.createNamedQuery(User.FIND_ALL_USERS, User.class).getResultList();
    }
    
    //----------------------USER-SPECIFIC--------------------------------------//
   
     /**
     * A path for users to sign up to an event. When signed up it will add the event to myeventlist
     * and add him/her-self to the attendee list
     * @param eventid
     * @return 
     */
    @PUT
    @Path("joinevent")
    @RolesAllowed({Group.USER})
    public Response joinEvent(@QueryParam("eventid")Long eventid){
        
        Event event = em.find(Event.class, eventid);
        if(event !=null){
            User user = this.getCurrentUser();
            event.addAttender(user);
            return Response.ok().build();
        }
        return Response.notModified().build();
    }
    
    
    @PUT
    @Path("leave")
    @RolesAllowed({Group.USER})
    public Response leaveEvent(@QueryParam("eventid")Long eventid){
        
        Event event = em.find(Event.class, eventid);
        if(event != null) {
            User user = this.getCurrentUser();
            event.removeAttender(user);
            return Response.ok().build();
        }
        return Response.notModified().build();
    }
    
    
    @GET
    @Path("myevents")
    @RolesAllowed({Group.USER})
    public List<Event> getEvents(@QueryParam("userid") Long userid) {
        User user = this.getCurrentUser();
        return user.getMyEvents();
    }
    
    
    /**
     * Short command to get the current user, since it's a common request
     * @return 
     */
        private User getCurrentUser(){
        return em.find(User.class, sc.getUserPrincipal().getName());
    }
}
