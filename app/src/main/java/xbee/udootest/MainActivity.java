package xbee.udootest;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import me.palazzetti.adktoolkit.AdkManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
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
    public TextView textMatrix;

    private AdkReadTask mAdkReadTask;

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
        textMatrix = (TextView) findViewById(R.id.textMatrix);

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

        String myString = "Confusion Matrix \n";
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

    public BufferedReader readFile(File f) {
        try {
            FileReader fileReader = new FileReader(f);
            return new BufferedReader(fileReader);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
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
        }
    }



}
