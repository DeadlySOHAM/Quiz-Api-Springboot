package quiz.services.db.models;

import java.util.ArrayList;



public class ResultViewUtil{
    public String quiz=null;
    public ArrayList<Question> questions = new ArrayList<Question>(0);
    private int marks = -1;

    public class Question{
        public int id;
        public String question;
        public ArrayList<String> selected = new ArrayList<String>(0),
                unselected = new ArrayList<String>(0);
        public boolean is_correct = true;


        public void add(String option, boolean is_selected, boolean is_correct){
            if(!is_correct && is_selected)
                this.is_correct = false;
            else if(is_correct && !is_selected)
                this.is_correct = false;

            if(is_selected)
                selected.add(option);
            else
                unselected.add(option);
        }

        public String toString(){
            return "{\n\t"+
                    "Id : "+this.id+"\n\t"+
                    "Question : "+this.question+"\n\t"+
                    "is Correct : "+this.is_correct+"\n\t"+
                    "Selected : "+this.selected+"\n\t"+
                    "Unselected : "+this.unselected+"\n\t"+
                    "\t}\n";
        }

    }

    public void add(int qs_id, String qs,String opt, boolean is_selected, boolean is_correct){

        if(this.questions==null){
            Question q = new Question();
            this.questions = new ArrayList<Question>(){{
                add(q);
            }};
            q.id = qs_id;
            q.question = qs;
            q.add(opt,is_selected,is_correct);
            return;
        }

        for(Question q : this.questions){
            if(q.id == qs_id){
                q.add(opt,is_selected,is_correct);
                return;
            }
        }

        Question q = new Question();
        this.questions.add(q);
        q.id = qs_id;
        q.question = qs;
        q.add(opt,is_selected,is_correct);
    }

    public int getScore(){
        if(marks != -1) return marks;
        marks = 0;
        for(Question qs : this.questions)
            marks += qs.selected.size()>0 && qs.is_correct ? 1 : 0;
        return marks;
    }

    @Override
    public String toString() {
        return "{\n"+
                "Quiz : "+this.quiz+"\n"+
                "Questions : "+this.questions+"\n"+
                "Score : "+this.getScore()+"\n"+
                "}";
    }

}
