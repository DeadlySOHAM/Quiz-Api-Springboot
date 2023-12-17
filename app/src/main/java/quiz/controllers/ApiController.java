package quiz.controllers;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import quiz.services.db.Database;
import quiz.services.db.models.CreateQuizUtils;
import quiz.services.db.models.DashboardDetails;
import quiz.services.db.models.ResultViewUtil;
import quiz.services.db.models.TakeQuizUtil;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping("/api")
@CrossOrigin
public class ApiController {
   
   @Autowired
   Database db;

   @GetMapping("/ping")
   public String home(
      @RequestHeader MultiValueMap<String,String> headers
   ){
      System.out.println("Recieved headers\n"+headers);
      this.db.check();
      return "Working...";
   }

   @PostMapping("/signup")
   public ResponseEntity<HashMap<String,String>> signUp(
      @RequestHeader HashMap<String,String> header,
      @RequestBody HashMap<String,String> body
   ){
      boolean is_teacher = body.get("is_teacher") == null ? false : body.get("is_teacher").equals("true");
      
      HashMap<String,String> response = new HashMap<String,String>(1){{
         put(
            "token",
            db.signUp(
                  body.get("mail"),
                  body.get("password"), 
                  body.get("name"), 
                  body.get("institution"),
                  is_teacher
               )
         );
      }};
      
      return response.get("token") == null ?
         new ResponseEntity<HashMap<String,String>>(response,HttpStatus.EXPECTATION_FAILED) :
         new ResponseEntity<HashMap<String,String>>(response,HttpStatus.CREATED);
   
   }
   
   @PostMapping("/signin")
   public HashMap<String,String> signIn(
      @RequestHeader HashMap<String,String> header,
      @RequestBody HashMap<String,String> body
   ){
      boolean is_Teacher = body.get("is_teacher")!=null ? body.get("is_teacher").equals("true") : false;
      return new HashMap<String,String>(1){{
         put("token",db.login(body.get("mail"), body.get("password"),is_Teacher));
      }};
   }

   @GetMapping("/logout")
   public HashMap<String,Boolean> postMethodName(@RequestHeader HashMap<String,String> header) {
      return new HashMap<String,Boolean>(1){{
         put("status",db.logout(header.get("auth_token")));
      }};
   }

   @GetMapping("/dashboard")
   public DashboardDetails getDashboard(
      @RequestHeader HashMap<String,String> header
   ){
      return this.db.getDashboardDetails(header.get("auth_token"));
   }

   @PostMapping("/createquiz")
   public HashMap<String,Integer> createQuiz(
      @RequestHeader HashMap<String,String> header,
      @RequestBody CreateQuizUtils body
   ){
      return new HashMap<String,Integer>(1){{put("quiz_id",db.createQuiz(header.get("auth_token"),body));}};
   }

   @DeleteMapping("/deletequiz")
   public HashMap<String,Boolean> deleteQuiz(
      @RequestHeader HashMap<String,String> header,
      @RequestBody HashMap<String,Integer> body
   ){
      return new HashMap<String,Boolean>(1){{
         put(
            "status",
            db.delete_quiz(
               header.get("auth_token"),
               body.get("quiz_id")
            ) == 1
         );
      }};
   }
   
   @PostMapping("/addstudents")
   public ResponseEntity<ArrayList<String>> addStudents(
      @RequestHeader HashMap<String,String> header,
      @RequestBody HashMap<String,Object> body
   ){
      String auth_token = header.get("auth_token");
      int quiz_id = (Integer)body.get("quiz_id");
      try {
         for(String student : (ArrayList<String>)body.get("students"))
            this.db.addStudentToQuiz(auth_token, quiz_id, student);         
      } catch (Exception e) {
         return new ResponseEntity<ArrayList<String>>(this.getStudents(header, quiz_id),HttpStatus.BAD_REQUEST);
      }
      return new ResponseEntity<ArrayList<String>>(this.getStudents(header, quiz_id),HttpStatus.ACCEPTED);
   }

   @DeleteMapping("/removestudents")
   public ResponseEntity<ArrayList<String>> removeStudents(
      @RequestHeader HashMap<String,String> header,
      @RequestBody HashMap<String,Object> body
   ){
      String auth_token = header.get("auth_token");
      int quiz_id = (Integer)body.get("quiz_id");
      try{
         for(String student : (ArrayList<String>)body.get("students"))
            this.db.removeStudentFromQuiz(auth_token, quiz_id, student);
      }catch(Exception e){
         return new ResponseEntity<ArrayList<String>>(this.getStudents(header, quiz_id),HttpStatus.BAD_REQUEST);
      }
      return new ResponseEntity<ArrayList<String>>(this.getStudents(header, quiz_id),HttpStatus.ACCEPTED);
   }

   @GetMapping("/students")
   public ArrayList<String> getStudents(
      @RequestHeader HashMap<String,String> header,
      int quiz_id
   ){
      return this.db.getStudents(header.get("auth_token"), quiz_id);
   }

   @GetMapping("/takequiz")
   public TakeQuizUtil takeQuiz(
      @RequestHeader HashMap<String,String> header,
      int quiz_id
   ){
      return this.db.takeQuiz(header.get("auth_token"), quiz_id);
   }

   @PostMapping("/submitquiz")
   public HashMap<String,Boolean> submitQuiz(
      @RequestHeader HashMap<String,String> header,
      @RequestBody TakeQuizUtil tq
   ){
      System.out.println("Submitting ...\n"+tq);
      return new HashMap<String,Boolean>(1){{
         put(
            "status",
            db.submit_quiz(header.get("auth_token"), tq)
         );
      }};
   }

   @GetMapping("/result")
   public ResultViewUtil getResult(
      @RequestHeader HashMap<String,String> header,
      int quiz_id,
      String student_mail
   ){
      if(student_mail==null)
         return this.db.student_response_view(header.get("auth_token"), quiz_id);
      else{
         return this.db.teacher_response_view(header.get("auth_token"), quiz_id, student_mail);
      }
   }
   
}
