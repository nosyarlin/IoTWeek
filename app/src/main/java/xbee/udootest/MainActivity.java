package xbee.udootest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import me.palazzetti.adktoolkit.AdkManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import wlsvm.WLSVM;






public class MainActivity extends Activity{

//	private static final String TAG = "UDOO_AndroidADKFULL";	 

    private AdkManager mAdkManager;

    private ToggleButton buttonLED;
    private TextView distance;
    private TextView pulse;
    private TextView position;
    public TextView mentalState;

    public Button task1part2;

    private AdkReadTask mAdkReadTask;

    public static Instances testingSet;
    public FastVector fvWekaAttributes = null;

    float pulseRate;
    float oxygenLvl;

    WLSVM svmCls = null;

    public static final String svmModel = "fake_stressModel2";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdkManager = new AdkManager((UsbManager) getSystemService(Context.USB_SERVICE));

//		register a BroadcastReceiver to catch UsbManager.ACTION_USB_ACCESSORY_DETACHED action
        registerReceiver(mAdkManager.getUsbReceiver(), mAdkManager.getDetachedFilter());

        buttonLED = (ToggleButton) findViewById(R.id.toggleButtonLED);
        distance  = (TextView) findViewById(R.id.textView_distance);
        pulse  = (TextView) findViewById(R.id.textView_pulse);
        position  = (TextView) findViewById(R.id.textView_position);
        task1part2 = (Button) findViewById(R.id.buttonTask1);

        mentalState = (TextView) findViewById(R.id.textPrediction);

        task1part2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),Task1Part2Activity.class);
                startActivity(intent);
            }
        });

        // Goes to Downloads and finds the file stress_train.arff

        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File f = new File(root, "stress_train.arff");
        BufferedReader inputReader;

        // Creates a reader for later use
        inputReader = readFile(f);



        // Training with old data
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

    @Override
    public void onResume() {
        super.onResume();
        mAdkManager.open();

        mAdkReadTask = new AdkReadTask();
        mAdkReadTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdkManager.close();

        mAdkReadTask.pause();
        mAdkReadTask = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mAdkManager.getUsbReceiver());
    }


    // ToggleButton method - send message to SAM3X
    public void blinkLED(View v){
        if (buttonLED.isChecked()) {
            // writeSerial() allows you to write a single char or a String object.
            mAdkManager.writeSerial("1");
        } else {
            mAdkManager.writeSerial("0");
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

    public void TrainInput(double pulse, double oxygen, double position) {

        Attribute Attribute1 = new Attribute("pulse");
        Attribute Attribute2 = new Attribute("oxygen");
        Attribute Attribute3 = new Attribute("position");

        // Declare the class attribute along with its values(nominal)
        FastVector fvClassVal = new FastVector(2);

        fvClassVal.addElement("State-stressed");
        fvClassVal.addElement("State-notstressed");
        Attribute ClassAttribute = new Attribute("class", fvClassVal);

        // Declare the feature vector template

        FastVector fvWekaAttributes = new FastVector(4);
        fvWekaAttributes.addElement(Attribute1);
        fvWekaAttributes.addElement(Attribute2);
        fvWekaAttributes.addElement(Attribute3);
        fvWekaAttributes.addElement(ClassAttribute);


        // Creating testing instances object with name "TestingInstance"
        // using the feature vector template we declared above
        // and with initial capacity of 1

        Instances testingSet = new Instances("TestingInstance", fvWekaAttributes, 1);

        // Setting the column containing class labels:
        testingSet.setClassIndex(testingSet.numAttributes() - 1);

        // Create and fill an instance, and add it to the testingSet
        Instance iExample = new Instance(testingSet.numAttributes());
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), pulse);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), oxygen);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), position);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(3), "State-stressed"); // dummy

        // add the instance
        testingSet.add(iExample);

        String out = "";
        try{
            if (svmCls != null && testingSet != null) {
                double pred = svmCls.classifyInstance(testingSet.instance(0));

                //double pred = 0.0;

                Integer a = (int) pred;
                switch (a){
                    case 0:
                        out = "STRESSED";
                        break;
                    case 1:
                        out = "Not stressed";
                        break;
                    default:
                        out = String.valueOf(pred);
                        break;
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        mentalState.setText(out);

    }


    /*
     * We put the readSerial() method in an AsyncTask to run the
     * continuous read task out of the UI main thread
     */
    private class AdkReadTask extends AsyncTask<Void, String, Void> {

        private boolean running = true;

        public void pause(){
            running = false;
        }

        protected Void doInBackground(Void... params) {
            while(running) {
                publishProgress(mAdkManager.readSerial()) ;
            }
            return null;
        }

        protected void onProgressUpdate(String... progress) {

            pulseRate= (int)progress[0].charAt(0);
            oxygenLvl= (int)progress[0].charAt(1);
            float pos= (int)progress[0].charAt(2);
            int max = 255;
            if (pulseRate>max) pulseRate=max;
            if (oxygenLvl>max) oxygenLvl=max;
            if (pos>max) pos=max;

            TrainInput(pulseRate,oxygenLvl,pos);

//            DecimalFormat df = new DecimalFormat("#.#");
            distance.setText(pulseRate + " (bpm)");
            pulse.setText(oxygenLvl + " (pct)");
            position.setText(pos + "");

        }
    }



}
