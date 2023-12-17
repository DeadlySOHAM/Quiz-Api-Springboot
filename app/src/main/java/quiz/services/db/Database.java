package quiz.services.db;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import quiz.services.db.models.CreateQuizUtils;
import quiz.services.db.models.DashboardDetails;
import quiz.services.db.models.ResultViewUtil;
import quiz.services.db.models.TakeQuizUtil;

@Service("db")
public class Database {

    private static volatile Database Instance = null;

    private static final String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/quiz",
            Username = "Username",
            Password = "Password";

    private Connection _connection = null;

    public Database(){
        this.connect();
        Database.Instance = this;
    }

    public void check(){
        if(Instance != null)
            System.out.println("Done initializing");
        else
            System.out.println("Error initializing");
    }

    public boolean connect(){
        try {
            Class.forName("com.mysql.jdbc.Driver"); // Load the driver class
            this._connection = DriverManager.getConnection(jdbcUrl, Username, Password);
            return !this._connection.isClosed();
        } catch (Exception e) {
            System.out.println("Error in connecting\n"+e.toString());
            this._connection = null;
            return false;
        }
    }

    public boolean isConnected(){
        if(this._connection == null)
            return this.connect();
        try {
            if(this._connection.isClosed()) {
                System.out.println("Not connected, Trying to reconnect");
                return this.connect();
            }
            else return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public String login(String mail, String password, boolean isTeacher){

        String auth_token = null;
        
        System.out.println("Logging in : "+mail+"\t"+password+"\t"+isTeacher);

        if(!this.isConnected() ) {
            System.out.println("Null Connection");
            return null;
        }
        try {
            // Prepare the stored procedure call
            String call = isTeacher?
                    "{call teacher_login('"+mail+"', '"+password+"', ?)}":
                    "{call student_login('"+mail+"', '"+password+"', ?)}";

            CallableStatement callableStatement = this._connection.prepareCall(call);

            // Register the output parameter
            callableStatement.registerOutParameter(1, Types.VARCHAR);

            // Execute the stored procedure
            callableStatement.execute();

            // Get the output parameter value
            auth_token = callableStatement.getString(1);
            System.out.println("Auth Token : "+auth_token);

            // Close resources
            callableStatement.close();
        } catch (Exception e) {
            System.out.println("Error logging in\nauth_Token:"+auth_token+"\n"+e.toString());
        }
        return auth_token ;
    }

    public boolean logout(String auth_token){

        System.out.println("Logging out : "+auth_token);

        if(!this.isConnected() || auth_token == null ) return true;

        try{
            // Prepare the stored procedure call
            final String call = "{call logout('"+auth_token+"', ?)}";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            // Register the output parameter
            callableStatement.registerOutParameter(1, Types.TINYINT);


            // Execute the stored procedure
            callableStatement.execute();

            // Get the output parameter value
            int status = callableStatement.getInt(1);
            System.out.println("Status : "+ status);

            if(status == 1 ) auth_token = null;

            // Close resources
            callableStatement.close();
        }catch(Exception e){
            System.out.println("Error logging out\n"+e.toString());
        }
        return auth_token == null;
    }

    public String signUp(
            String mail, String password,
            String name, String institution,
            boolean is_teacher
    ){
        System.out.println("Signing Up\n"+mail+"\t"+name+"\t"+password+"\t"+institution);
        if(!this.isConnected() ) return null;
        String auth_token = null;
        try {
            // Prepare the stored procedure call
            String call = is_teacher ?
                "{call teacher_signUp_login('"+mail+"', '"+name+"' , '"+password+"', '"+institution+"' , ?)}":
                "{call student_signUp_login('"+mail+"', '"+name+"' , '"+password+"', ?)}";

            CallableStatement callableStatement = _connection.prepareCall(call);

            // Register the output parameter
            callableStatement.registerOutParameter(1, Types.VARCHAR);

            // Execute the stored procedure
            callableStatement.execute();

            // Get the output parameter value
            auth_token = callableStatement.getString(1);

            System.out.println("Signed Up : Auth Token : "+auth_token);

            // Close resources
            callableStatement.close();
        } catch (Exception e) {
            System.out.println("Error SignUp\n"+e.toString());
        }
        return auth_token;
    }

    public boolean isLogin(String auth_token){
        System.out.println("Checking Login stat");
        if (auth_token == null || !this.isConnected())   return false;
        int status = -1;
        try{
            final String call = "{ ? = call is_Login('"+auth_token+"') }";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.registerOutParameter(1, Types.INTEGER);

            callableStatement.execute();

            status = callableStatement.getInt(1);

            callableStatement.close();
        }catch(Exception e){
            System.out.println("Error checking login status\n"+e.toString());
        }
        System.out.println("Login Stat : "+status);
        return status == 1;
    }

    public boolean isTeacher(String auth_token){
        if (auth_token == null)   return false;
        else if(!this.isConnected()) return false;
        int status = -1;
        try{
            final String call = "{ ? = call is_Teacher('"+auth_token+"') }";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.registerOutParameter(1, Types.INTEGER);

            callableStatement.execute();

            status = callableStatement.getInt(1);

            callableStatement.close();

        }catch(Exception e){
            System.out.println("Error checking login status\n"+e.toString());
        }
        return status == 1;
    }

    public DashboardDetails getDashboardDetails(String auth_token){
        if(auth_token == null)return null;
        else if(!this.isConnected()) return null;
        DashboardDetails dd = null;
        try{
            final String call = "{call dashboard_details(\""+auth_token+"\")}";
            ResultSet result = this._connection.createStatement().executeQuery(call);
            ResultSetMetaData resultSetMetaData = result.getMetaData();

            ArrayList<String> cols = new ArrayList<String>(5);

            for ( int i=1; i<=resultSetMetaData.getColumnCount(); i++)
                cols.add(resultSetMetaData.getColumnName(i));

            dd = new DashboardDetails();
            dd.isTeacher = !cols.contains("teacher");
            dd.quizes = new ArrayList<DashboardDetails.Quiz>(0);

            while(result.next()){
                int quiz_id, question_count, total_attempt;
                String quiz_name;

                dd.name = result.getString("name");

                if(result.getString("quiz") == null) continue;

                quiz_id = result.getInt("quiz_id");
                quiz_name = result.getString("quiz");
                question_count = result.getInt("question_count");
                total_attempt = result.getInt("total_attempt") ;

                DashboardDetails.Quiz qz = dd.new Quiz(quiz_id,quiz_name,question_count,total_attempt);
                dd.quizes.add(qz);

                if(!cols.contains("teacher")) continue;

                qz.Teacher = result.getString("teacher");
                qz.attemptCount = result.getInt("attempt_count") ;
            }
            cols = null;
        }catch(Exception e){
            System.out.println("Error getting dashboard details\n"+e.toString());
        }
        return dd;
    }

    public int createQuiz(String auth_token, CreateQuizUtils data){
        int quiz_id = -1;
        if(!this.isConnected()) return -1;
        try{
            data.total_attempt = data.total_attempt == 0 ? 1 : data.total_attempt;
            final String call = "{call create_quiz('"+auth_token+"','"+data.name+"','"+data.total_attempt+"',?)}";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.registerOutParameter(1, Types.INTEGER);

            callableStatement.execute();

            quiz_id = callableStatement.getInt(1);
            callableStatement.close();

            System.out.println("Quiz id : "+quiz_id);

            if(quiz_id == -1) return -1;

            for(CreateQuizUtils.Question q : data.questions){
                int question_id = this.insertQuestion(auth_token, quiz_id,q.question);
                if(question_id == -1) return -1;
                for(CreateQuizUtils.Question.Option opt : q.options)
                    this.insertOption(auth_token, quiz_id,question_id,opt.option,opt.is_correct);
            }

        }catch(Exception e){
            System.out.println("adding Quiz\n"+e.toString());
        }
        return quiz_id;
    }

    public int delete_quiz(String auth_token, int quiz_id){
        int status = 0;
        if(!this.isConnected()) return 0;
        try {
            final String call = "{call delete_quiz('" + auth_token + "','" + quiz_id + "',?)}";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.registerOutParameter(1, Types.INTEGER);

            callableStatement.execute();

            status = callableStatement.getInt(1);

            callableStatement.close();
        }catch(Exception e){
            System.out.println("Error Deleting Quiz : "+e.toString());
        }
        return status;
    }

    private int insertQuestion(String auth_token, int quiz_id,String question){
        int question_id = -1;
        if(!this.isConnected()) return -1;
        try{
            final String call = "{call insert_question('"+auth_token+"','"+quiz_id+"','"+question+"',?)}";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.registerOutParameter(1, Types.INTEGER);

            callableStatement.execute();

            question_id = callableStatement.getInt(1);
            callableStatement.close();
            System.out.println("Question id : "+question_id);

        }catch(Exception e){
            System.out.println("adding Question\n"+e.toString());
        }
        return question_id;
    }

    private int insertOption(String auth_token, int quiz_id,int question_id,String value, boolean is_correct){
        int option_id = -1;
        if(!this.isConnected()) return -1;
        try{
            int isCorrect = is_correct ? 1 : 0;
            final String call =
                    "{call insert_option('"+auth_token+"','"+quiz_id+"','"+question_id+"','"+value+"','"+isCorrect+"',?)}";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.registerOutParameter(1, Types.INTEGER);

            callableStatement.execute();

            option_id = callableStatement.getInt(1);
            callableStatement.close();
            System.out.println("Option id : "+quiz_id);
        }catch(Exception e){
            System.out.println("Inserting Question :\n"+e.toString());
        }
        return option_id;
    }

    public String addStudentToQuiz(String auth_token, int quiz_id, String student_mail){
        String status = null;
        if(!this.isConnected()) return null;
        try{
            final String call =
                    "{call add_student_to_quiz('"+auth_token+"','"+quiz_id+"','"+student_mail+"',?)}";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.registerOutParameter(1, Types.INTEGER);

            callableStatement.execute();

            status = callableStatement.getString(1);
            callableStatement.close();
            System.out.println("Added Student to quiz : "+status+"|"+student_mail);
        }catch(Exception e){
            System.out.println("Not added Student to quiz : "+status+"|"+student_mail+"\n"+e.toString());
        }
        return status;
    }

    public int removeStudentFromQuiz(String auth_token, int quiz_id, String student_mail) {
        int status = -1;
        if(!this.isConnected()) return -1;
        try{
            final String call =
                    "{call remove_student_from_quiz('"+auth_token+"','"+quiz_id+"','"+student_mail+"',?)}";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.registerOutParameter(1, Types.INTEGER);

            callableStatement.execute();

            status = callableStatement.getInt(1);
            callableStatement.close();
            System.out.println("Deleting Student from quiz : "+status+"|"+student_mail);
        }catch(Exception e){
            System.out.println("Not Deleting Student from quiz : "+status+"|"+student_mail+"\n"+e.toString());
        }
        return status;
    }

    public ArrayList<String> getStudents(String auth_token, int quiz_id){
        ArrayList<String> studentList = new ArrayList<String>(0);
        if(!this.isConnected()) return studentList;
        try{
            if(!this.isTeacher(auth_token)) return studentList;
            final String call = "call get_students('"+auth_token+"','"+quiz_id+"')";
            ResultSet result = this._connection.createStatement().executeQuery(call);
            while(result.next())
                studentList.add(result.getString("_student_mail"));
        }catch(Exception e){
            System.out.println("Error getting student lis\nt"+e.toString());
        }
        return studentList;
    }

    public TakeQuizUtil takeQuiz(String auth_token, int quiz_id){
        TakeQuizUtil tq = new TakeQuizUtil();
        if(!this.isConnected()) return tq;
        try{
            final String call = "{call take_quiz('"+auth_token+"','"+quiz_id+"')}";
            ResultSet table = this._connection.createStatement().executeQuery(call);

            System.out.println("Taking Quiz :");
            while(table.next()){
                tq.session = table.getString("session");
                tq.quiz = table.getString("quiz");
                tq.id = table.getInt("quiz_id");
                tq.add(
                        table.getInt("question_id"),
                        table.getInt("option_id"),
                        table.getString("question"),
                        table.getString("option")
                );
            }
            System.out.println(tq.toString());
        }catch(Exception e){
            System.out.println("Error Taking Quiz"+"\n"+e.toString());
        }
        return tq;
    }

    public boolean submit_quiz(String auth_token, TakeQuizUtil tq){
        if(!this.isConnected()) return false;
        for(int qs=0; qs<tq.questions.size(); qs++) {
            for (int opt = 0; opt < tq.questions.get(qs).options.size(); opt++) {
                if (tq.questions.get(qs).options.get(opt).is_selected) {
                    try {
                        final String queryString = "{call submit_answer('" + auth_token + "','" +tq.session + "','"+ tq.id + "','" + tq.questions.get(qs).id + "','" + tq.questions.get(qs).options.get(opt).id + "',?)}";
                        CallableStatement callableStatement = this._connection.prepareCall(queryString);
                        callableStatement.registerOutParameter(1, Types.INTEGER);
                        callableStatement.execute();
                        System.out.println("Submitted answer.");
                        System.out.println( tq.questions.get(qs).question + "\t" + tq.questions.get(qs).options.get(opt).option + "\t" + callableStatement.getInt(1));
                        if(callableStatement.getInt(1)==-1)
                            return false;
                    } catch (Exception e) {
                        System.out.println( "Error submitting quiz\n" + e);
                        System.out.println( tq.questions.get(qs).question + "\t" + tq.questions.get(qs).options.get(opt).option);
                    }
                }
            }
        }
        this.deleteSession(auth_token, tq.session, tq.id);
        return true;
    }

    private void deleteSession(String auth_token,String session_id, int quiz_id){
        if(!this.isConnected()) return ;
        try{
            final String call =
                    "{call delete_session('"+auth_token+"','"+session_id+"',"+quiz_id+")}";
            CallableStatement callableStatement = this._connection.prepareCall(call);

            callableStatement.execute();

            callableStatement.close();
            System.out.println("Deleting Session "+session_id);
        }catch(Exception e){
            System.out.println("Not Deleting Session "+session_id);
            e.printStackTrace();
        }
        return ;
    }

    public ResultViewUtil student_response_view(String auth_token, int quiz_id){
        ResultViewUtil rvu = new ResultViewUtil();
        if(!this.isConnected()) return rvu;
        if(this.isTeacher(auth_token)) return rvu;
        try{
            final String call ="{call student_response_view('"+auth_token+"','"+quiz_id+"')}";
            ResultSet table = _connection.createStatement().executeQuery(call);
            while(table.next()){
                rvu.quiz = table.getString("quiz");
                rvu.add(
                        table.getInt("question_id"),
                        table.getString("question"),
                        table.getString("option"),
                        table.getInt("selected")!=0,
                        table.getInt("is_correct")==1
                );
            }
            System.out.println("Student Response : "+rvu);
        }catch(Exception e){
            System.out.println("Error getting response view for student");
        }
        return rvu;
    }

    public ResultViewUtil teacher_response_view(String auth_token, int quiz_id, String student_mail){
        ResultViewUtil rvu = new ResultViewUtil();
        if(!this.isConnected()) return rvu;
        if(!this.isTeacher(auth_token)) return rvu;
        try{
            final String call ="{call teacher_response_view('"+auth_token+"','"+quiz_id+"','"+student_mail+"')}";
            ResultSet table = _connection.createStatement().executeQuery(call);

            while(table.next()){
                rvu.quiz = table.getString("quiz");
                rvu.add(
                        table.getInt("question_id"),
                        table.getString("question"),
                        table.getString("option"),
                        table.getInt("selected")!=0,
                        table.getInt("is_correct")==1
                );
            }
            System.out.println("Teacher view :\n"+rvu);
        }catch(Exception e){
            System.out.println("Error getting response view for teacher");
        }
        return rvu;
    }

    public boolean close(){
        try {
            if(!this.isConnected())
                this._connection.close();
            System.out.println("Closed connection");
            return true;
        } catch (Exception e) {
            System.out.println("Error closing connection\n"+e.toString());
            return false;
        }
    }

}