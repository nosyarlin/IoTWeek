package xbee.udootest;

import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Instances;

public class Main2Activity extends Activity {

    public TextView textMatrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Goes to Downloads and finds the file iris_train.arff
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File f = new File(root, "iris_train.arff");
        BufferedReader inputReader;

        // Creates a reader for later use
        inputReader = readFile(f);  // you need to code the readFile() that takes in a File object and returns a BufferedReader
        int correct = 0, incorrect=0;
        Integer[][] matrix = {{0,0,0},{0,0,0},{0,0,0}};
        // No idea what's going on
        try {

            Instances data = new Instances(inputReader);
            data.setClassIndex(data.numAttributes() - 1);
            Classifier ibk = new IBk();
            ibk.buildClassifier(data);
            f = new File(root, "iris_test.arff");
            inputReader = readFile(f);
            Instances test = new Instances(inputReader);
            test.setClassIndex(test.numAttributes() -1);
            for (int i = 0; i < test.numInstances(); i++) {
                // Compare the prediction results with the actual class label
                double pred = ibk.classifyInstance(test.instance(i));
                double act = test.instance(i).classValue();
                if(pred == act) correct += 1;
                else incorrect += 1;

                if (matrix[(int)pred][(int)act] == null) {
                    matrix[(int)pred][(int)act] = 0;
                }
                else {
                    matrix[(int)pred][(int)act] += 1;
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        String myString = "Confusion Matrix \n a   b  c   <-- Classified as\n";
        for (Integer[] row:matrix) {
            for (int col:row){
                myString = myString + String.valueOf(col) + "  ";
            }
            myString += "\n";
        }


        textMatrix.setText(myString);
        // report the number of correct and incorrect
        Toast.makeText(this,"correct: "+correct+ " incorrect: "+ incorrect,Toast.LENGTH_LONG*100).show();
        Log.i("test", "correct: "+correct+ " incorrect: "+ incorrect);
    }

    public BufferedReader readFile(File f) {
        try {
            FileReader fileReader = new FileReader(f);
            return new BufferedReader(fileReader);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

}
