package quiz.services.db.models;

import java.util.ArrayList;

public class DashboardDetails {

    public String name;
    public boolean isTeacher;
    public ArrayList<Quiz> quizes;

    @Override
    public String toString() {
       return "{\nname :"+this.name+
               "\nisTeacher : "+this.isTeacher+
               "\nQuizes :\n"+this.quizes+
        "\n}";
    }

    public class Quiz{
        public int id, _question_count;
        public String Teacher,Name;
        public int attemptCount,total_attempt;

        public Quiz(int id,String Name, int question_count, int total_attempt){
            this.id = id;
            this.Name = Name;
            this._question_count = question_count;
            this.total_attempt = total_attempt;
        }

        @Override
        public String toString() {
            return "{\nid : " + this.id+
                    "\nTeacher : "+ this.Teacher+
                    "\nName : "+ this.Name+
                    "\nattemptCount :  "+this.attemptCount +
                    "\ntotalAttempt :  "+this.total_attempt +
                    "\nQuestion Count : "+this._question_count+
                    "\n}";
        }
    }

}
