package overdector.com.overdrawdector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;

import overdector.com.bean.RGBbean;
import overdector.com.data.ColorAdapter;

/**
 * Created by mozzie on 17/9/1.
 */

public class ImageViewActivity extends Activity implements View.OnTouchListener, View.OnLongClickListener, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemLongClickListener{

    private ImageView mEditImg;
    Bitmap bitmap = null;
    private CheckBox mCheckbox;
    private Button mEnsureBtn;
    private float mCurrentX, mCurrentY;
    private int r,g,b;
    private ArrayList<RGBbean> mRGBbean;
    private ListView mColorListView;
    private ColorAdapter mColorAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);
        mRGBbean = new ArrayList<>();
        mEditImg = (ImageView) findViewById(R.id.overdraw_image);
        mCheckbox = (CheckBox) findViewById(R.id.overdraw_check);
        mEnsureBtn = (Button) findViewById(R.id.overdraw_edit_ensure);
        mCheckbox.setOnCheckedChangeListener(this);

        Uri uri = (Uri) getIntent().getParcelableExtra("image");
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mEditImg.setImageBitmap(bitmap);
        mEditImg.setOnTouchListener(this);
        mEditImg.setOnLongClickListener(this);

        mEnsureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("rgblist", mRGBbean);
                getIntent().putExtras(bundle);
                setResult(RESULT_OK, getIntent());
                finish();
            }
        });

        mColorListView = (ListView) findViewById(R.id.overdraw_listview);
        mColorAdapter = new ColorAdapter(getBaseContext());
        mColorListView.setAdapter(mColorAdapter);
        mColorListView.setOnItemLongClickListener(this);
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                Log.d("huhao-ontouch", "currentX =" + event.getX() + ", currentY =" + event.getY()) ;
                mCurrentX = event.getX();
                mCurrentY = event.getY();
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.d("huhao-ontouch", "cancel");
                break;
            case MotionEvent.ACTION_BUTTON_PRESS:
                Log.d("huhao-ontouch", "press");
                break;
            case MotionEvent.ACTION_UP:
                Log.d("huhao-ontouch", "up");
                break;
        }
        return false;
    }

    /**
     * Called when a view has been clicked and held.
     *
     * @param v The view that was clicked and held.
     * @return true if the callback consumed the long click, false otherwise.
     */
    @Override
    public boolean onLongClick(View v) {
        Log.d("huhao-onLongClick", "width = " + v.getWidth() + ", height = " + v.getHeight()) ;
        Log.d("huhao-onLongClick", "bitmap = " + bitmap.getWidth() + "," + bitmap.getHeight()) ;
        int color = bitmap.getPixel((int)mCurrentX,(int)mCurrentY);
        r = Color.red(color);
        g = Color.green(color);
        b = Color.blue(color);
        mCheckbox.setText(r + "," + g + "," + b);
        mCheckbox.setChecked(false);
        mCheckbox.setVisibility(View.VISIBLE);
        mCheckbox.setBackgroundColor(color);
        Log.d("huhao-onLongClick", "RGB = " + r + "," + g + "," + b) ;

        mCurrentX = 0;
        mCurrentY = 0;
        return false;
    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView The compound button view whose state has changed.
     * @param isChecked  The new checked state of buttonView.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked){
            mCheckbox.setChecked(false);
            mCheckbox.setVisibility(View.INVISIBLE);
            RGBbean rgBbean = new RGBbean();
            rgBbean.setR(r);
            rgBbean.setG(g);
            rgBbean.setB(b);
            if(mRGBbean.size() == 0){
                mRGBbean.add(rgBbean);
            }else{
                boolean isAdded = true;
                for(int i = 0 ; i < mRGBbean.size(); i++){
                    if(mRGBbean.get(i).getR() == rgBbean.getR() &&
                            mRGBbean.get(i).getG() == rgBbean.getG() &&
                            mRGBbean.get(i).getB() == rgBbean.getB()){
                        isAdded = false;
                    }
                }
                if(isAdded){
                    mRGBbean.add(rgBbean);
                    isAdded = true;
                }
            }

            Log.d("huhao-onCheckedChanged", "RGB = " + r + "," + g + "," + b) ;
            notifyListDataChange();

        }
    }

    private void notifyListDataChange() {
        mColorAdapter.setRGBBean(mRGBbean);
        mColorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if(mCheckbox.getVisibility() == View.VISIBLE){
            mCheckbox.setVisibility(View.INVISIBLE);
            mCheckbox.setChecked(false);
        }else{
            finish();
        }
    }

    /**
     * Callback method to be invoked when an item in this view has been
     * clicked and held.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need to access
     * the data associated with the selected item.
     *
     * @param parent   The AbsListView where the click happened
     * @param view     The view within the AbsListView that was clicked
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     * @return true if the callback consumed the long click, false otherwise
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mRGBbean != null && mRGBbean.size() > position){
            mRGBbean.remove(position);
            notifyListDataChange();
        }
        return false;
    }
}
