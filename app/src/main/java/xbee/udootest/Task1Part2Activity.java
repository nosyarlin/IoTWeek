package xbee.udootest;

import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import weka.core.Instance;
import weka.core.Instances;
import wlsvm.WLSVM;

public class Task1Part2Activity extends Activity {

    public EditText editSepalLength;
    public EditText editSepalWidth;
    public EditText editPetalLength;
    public EditText editPetalWidth;

    public Button buttonTrain;
    public Button buttonClassify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task1_part2);

        editSepalLength = (EditText) findViewById(R.id.editSepalLength);
        editSepalWidth = (EditText) findViewById(R.id.editSepalWidth);
        editPetalLength = (EditText) findViewById(R.id.editPetalLength);
        editPetalWidth = (EditText) findViewById(R.id.editPetalWidth);

        buttonTrain = (Button) findViewById(R.id.buttonTrain);
        buttonClassify = (Button) findViewById(R.id.buttonClassify);

        // Goes to Downloads and finds the file iris_train.arff
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File f = new File(root, "iris_train.arff");
        BufferedReader inputReader;

        // Creates a reader for later use
        inputReader = readFile(f);  // you need to code the readFile() that takes in a File object and returns a BufferedReader

        WLSVM svmCls = new WLSVM();

        // Training with old data
        try {
            Instances data = new Instances(inputReader);
            data.setClassIndex(data.numAttributes() - 1);
            svmCls.buildClassifier(data);
            weka.core.SerializationHelper.write(svmModel, svmCls);
        } catch (Exception e){
            e.printStackTrace();
        }








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
