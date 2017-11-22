package xbee.udootest;

import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import weka.core.Attribute;
import weka.core.FastVector;
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

    WLSVM svmCls = null;
    public static final String svmModel = "original_svmModel";

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

        // Training with old data
        try {
            svmCls = (WLSVM) weka.core.SerializationHelper.read(svmModel);

        } catch (Exception e){
            svmCls = new WLSVM();

            try {
                Instances data = new Instances(inputReader);
                data.setClassIndex(data.numAttributes() - 1);
                svmCls.buildClassifier(data);
                weka.core.SerializationHelper.write(svmModel, svmCls);

            } catch (Exception ex){
                ex.printStackTrace();
            }
        }

        buttonTrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editPetalLength.getText().toString().isEmpty()
                        && !editPetalWidth.getText().toString().isEmpty()
                        && !editSepalLength.getText().toString().isEmpty()
                        && !editSepalWidth.getText().toString().isEmpty()) {
                    double slValue = Double.valueOf(editSepalLength.getText().toString());
                    double swValue = Double.valueOf(editSepalWidth.getText().toString());
                    double plValue = Double.valueOf(editPetalLength.getText().toString());
                    double pwValue = Double.valueOf(editPetalWidth.getText().toString());

                    TrainInput(slValue,swValue,plValue,pwValue);
                }
            }
        });

        buttonClassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editPetalLength.getText().toString().isEmpty()
                        && !editPetalWidth.getText().toString().isEmpty()
                        && !editSepalLength.getText().toString().isEmpty()
                        && !editSepalWidth.getText().toString().isEmpty()) {
                    double slValue = Double.valueOf(editSepalLength.getText().toString());
                    double swValue = Double.valueOf(editSepalWidth.getText().toString());
                    double plValue = Double.valueOf(editPetalLength.getText().toString());
                    double pwValue = Double.valueOf(editPetalWidth.getText().toString());

                    Instances test = TrainInput(slValue,swValue,plValue,pwValue);
                    try{
                        double pred = svmCls.classifyInstance(test.instance(0));
                        String out = "";

                        switch ((int) pred){
                            case 0:
                                out = "Iris-setosa";
                                break;
                            case 1:
                                out = "Iris-versicolor";
                                break;
                            case 2:
                                out = "Iris-virginica";
                                break;
                        }
                        Toast.makeText(v.getContext(),out,Toast.LENGTH_SHORT).show();
                        editPetalWidth.setText("");
                        editPetalLength.setText("");
                        editSepalWidth.setText("");
                        editSepalLength.setText("");

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });
    }



    public Instances TrainInput(double slValue, double swValue, double plValue, double pwValue) {
        Attribute Attribute1 = new Attribute("sepallength");
        Attribute Attribute2 = new Attribute("sepalwidth");
        Attribute Attribute3 = new Attribute("petallength");
        Attribute Attribute4 = new Attribute("petalwidth");

        // Declare the class attribute along with its values(nominal)

        FastVector fvClassVal = new FastVector(3);

        fvClassVal.addElement("Iris-setosa");
        fvClassVal.addElement("Iris-versicolor");
        fvClassVal.addElement("Iris-virginica");
        Attribute ClassAttribute = new Attribute("class", fvClassVal);

        // Declare the feature vector template

        FastVector fvWekaAttributes = new FastVector(5);
        fvWekaAttributes.addElement(Attribute1);
        fvWekaAttributes.addElement(Attribute2);
        fvWekaAttributes.addElement(Attribute3);
        fvWekaAttributes.addElement(Attribute4);
        fvWekaAttributes.addElement(ClassAttribute);


        // Creating testing instances object with name "TestingInstance"
        // using the feature vector template we declared above
        // and with initial capacity of 1

        Instances testingSet = new Instances("TestingInstance", fvWekaAttributes, 1);

        // Setting the column containing class labels:
        testingSet.setClassIndex(testingSet.numAttributes() - 1);

        // Create and fill an instance, and add it to the testingSet
        Instance iExample = new Instance(testingSet.numAttributes());
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), slValue);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), swValue);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), plValue);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(3), pwValue);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(4), "Iris-setosa"); // dummy

        // add the instance
        testingSet.add(iExample);
        return testingSet;
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
