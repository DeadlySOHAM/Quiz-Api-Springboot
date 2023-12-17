package quiz.services.db.models;

import java.util.ArrayList;


public class CreateQuizUtils {

    public String name = "";

    public int current = -1;

    public ArrayList<Question> questions ;

    public int total_attempt = 1;


    static public class Question{
    
        public String question = "" ;
    
        public ArrayList<Option> options = null ;
    

        static public class Option{
            public String option = "";
            public boolean is_correct = false;
        
            @Override
            public String toString() {
                return " {"+this.option+"("+this.is_correct+")} ";
            }
        }


        public Question(){
            this.options = new ArrayList<Option>(0);
        }
    
        public ArrayList<Option> addNewOption(){
            this.options.add(new Option());
            return this.options;
        }
    
        public void deleteOption(int i){
            this.options.remove(i);
        }
    
        @Override
        public String toString() {
            return "\t{\n\tQuestion : "+ this.question+
                    "\n\tOptions : "+this.options+
                    "\n\t}";
        }
    }

    public CreateQuizUtils(){
        this.questions = new ArrayList<Question>(0);
    }

    public Question getNext(){
        if( this.current == -1 || this.current == this.questions.size() -1 ) {
            Question qs = new Question();
            this.questions.add(qs);
        }
        return this.questions.get(++this.current);
    }

    public Question getPrevious(){
        if(this.current < 1 ) return null;
        if(this.current > this.questions.size()) return null;
        return this.questions.get(--this.current);
    }

    public void deleteCurrent(){
        if(this.current>=this.questions.size()) return;
        this.questions.remove(this.current);
    }

    public Question getCurrent(){
        if(this.current == -1)return null;
        if(this.current >= this.questions.size() ) return null;
        return this.questions.get(this.current);
    }

    @Override
    public String toString() {
        return "{\nName : " +this.name+
                "\nQuestions:\n"+this.questions+
                "\n}";
    }

    public Question validate() {
        for(Question q : this.questions){
            if(q.question.length()<1 && q.options.size()<2) {
                this.questions.remove(q);
                continue;
            }else if(q.options.size()>1){
                boolean isAtleastTrue = false;
                for(Question.Option opt : q.options) {
                    if (opt.option.length() < 1) return q;
                    isAtleastTrue = isAtleastTrue ? true : opt.is_correct;
                }
                if(!isAtleastTrue) return q;
            }else return q;
        }
        return null;
    }
}