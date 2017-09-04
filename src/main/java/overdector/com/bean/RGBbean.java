package overdector.com.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by mozzie on 17/9/1.
 */

public class RGBbean implements Parcelable {
    private int r;
    private int g;
    private int b;

    public static final Creator<RGBbean> CREATOR = new Creator<RGBbean>() {
        @Override
        public RGBbean createFromParcel(Parcel source) {
            RGBbean rgBbean = new RGBbean();
            rgBbean.setR(source.readInt());
            rgBbean.setG(source.readInt());
            rgBbean.setB(source.readInt());
            return rgBbean;
        }

        @Override
        public RGBbean[] newArray(int size) {
            return new RGBbean[size];
        }
    };

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public int getG() {
        return g;
    }

    public void setG(int g) {
        this.g = g;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable
     * instance's marshaled representation. For example, if the object will
     * include a file descriptor in the output of {@link #writeToParcel(Parcel, int)},
     * the return value of this method must include the
     * {@link #CONTENTS_FILE_DESCRIPTOR} bit.
     *
     * @return a bitmask indicating the set of special object types marshaled
     * by this Parcelable object instance.
     * @see #CONTENTS_FILE_DESCRIPTOR
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param parcel  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(r);
        parcel.writeInt(g);
        parcel.writeInt(b);
    }
}
