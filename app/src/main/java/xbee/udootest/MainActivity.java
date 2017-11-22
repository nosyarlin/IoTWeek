package xbee.udootest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
    public Button buttonStress;
    public Button buttonNotStress;
    public Button buttonTrain;

    private AdkReadTask mAdkReadTask;

    public static Instances testingSet;
    public FastVector fvWekaAttributes = null;

    WLSVM svmCls = null;
    public static final String svmModel = "original_stressModel";
    public static final String stress = "Stressed";
    public static final String not_stress = "Not Stressed";

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
        buttonStress = (Button) findViewById(R.id.buttonStress);
        buttonNotStress = (Button) findViewById(R.id.buttonNotStress);
        buttonTrain = (Button) findViewById(R.id.buttonSaveData);
        mentalState = (TextView) findViewById(R.id.textPrediction);

        task1part2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),Task1Part2Activity.class);
                startActivity(intent);
            }
        });

        Attribute Attribute1 = new Attribute("pulse");
        Attribute Attribute2 = new Attribute("oxygen");
        Attribute Attribute3 = new Attribute("position");

        // Declare the class attribute along with its values(nominal)

        FastVector fvClassVal = new FastVector(2);

        fvClassVal.addElement("Stressed");
        fvClassVal.addElement("Not Stressed");
        Attribute ClassAttribute = new Attribute("class", fvClassVal);

        // Declare the feature vector template

        fvWekaAttributes = new FastVector(4);
        fvWekaAttributes.addElement(Attribute1);
        fvWekaAttributes.addElement(Attribute2);
        fvWekaAttributes.addElement(Attribute3);
        fvWekaAttributes.addElement(ClassAttribute);

        // Creating testing instances object with name "TestingInstance"
        // using the feature vector template we declared above
        // and with initial capacity of 1

        //testingSet is actually trainingSet but we lazy
        testingSet = new Instances("TestingInstance", fvWekaAttributes, 50);

        // Setting the column containing class labels:
        testingSet.setClassIndex(testingSet.numAttributes() - 1);

        // Clicking the stressed button adds current result as stressed to training data
        buttonStress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToTrainingSet(stress);
            }
        });

        // Clicking not stressed button adds current result as not stressed to training data
        buttonNotStress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToTrainingSet(not_stress);
            }
        });

        buttonTrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                svmCls = new WLSVM();

                try {
                    //MainActivity.testingSet.setClassIndex(MainActivity.testingSet.numAttributes() - 1);
                    svmCls.buildClassifier(MainActivity.testingSet);
                    weka.core.SerializationHelper.write(svmModel, svmCls);

                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        // Training with old data
        try {
            svmCls = (WLSVM) weka.core.SerializationHelper.read(svmModel);
        } catch (Exception e){
            e.printStackTrace();
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
    public void TrainInput(double pulse, double oxygen, double position, String state) {

        // Create and fill an instance, and add it to the testingSet
        Instance iExample = new Instance(testingSet.numAttributes());
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), pulse);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), oxygen);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), position);
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(3), state);

        // add the instance
        testingSet.add(iExample);
    }

    public void addToTrainingSet(String state) {
        String mDistance = distance.getText().toString();
        String mPulse = pulse.getText().toString();
        String mPosition = position.getText().toString();
        if (!mDistance.isEmpty()
                && !mPulse.isEmpty()
                && !mPosition.isEmpty()) {
            TrainInput(Double.valueOf(mDistance),Double.valueOf(mPulse),Double.valueOf(mPosition), state);
        }
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
//	    	Log.i("ADK demo bi", "start adkreadtask");
            while(running) {
                publishProgress(mAdkManager.readSerial()) ;
            }
            return null;
        }

        protected void onProgressUpdate(String... progress) {

            float pulseRate= (int)progress[0].charAt(0);
            float oxygenLvl= (int)progress[0].charAt(1);
            float pos= (int)progress[0].charAt(2);
            int max = 255;
            if (pulseRate>max) pulseRate=max;
            if (oxygenLvl>max) oxygenLvl=max;
            if (pos>max) pos=max;

//            DecimalFormat df = new DecimalFormat("#.#");
            distance.setText(pulseRate + " (bpm)");
            pulse.setText(oxygenLvl + " (pct)");
            position.setText(pos + "");

            // Create new instance to be classified
            Instance iExample = new Instance(testingSet.numAttributes());
            iExample.setValue((Attribute)fvWekaAttributes.elementAt(0), pulseRate);
            iExample.setValue((Attribute)fvWekaAttributes.elementAt(1), oxygenLvl);
            iExample.setValue((Attribute)fvWekaAttributes.elementAt(2), pos);
            iExample.setValue((Attribute)fvWekaAttributes.elementAt(3), not_stress); //dummy

            try{
                int pred = (int) svmCls.classifyInstance(iExample);
                switch (pred){
                    case 0:
                        mentalState.setText(stress);
                        break;
                    case 1:
                        mentalState.setText(not_stress);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



}
