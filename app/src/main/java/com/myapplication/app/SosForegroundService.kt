package com.myapplication.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.BuildConfig
import android.widget.Toast
import com.myapplication.app.MainActivity
import com.myapplication.app.ml.FallDetectionModel

import kotlin.math.sqrt
import kotlin.random.Random
import android.util.Log
val realFallWindow = arrayOf(
    floatArrayOf(-0.134150f, -0.977326f, 0.095696f),
    floatArrayOf(-0.138582f, -0.974456f, 0.082718f),
    floatArrayOf(-0.130097f, -0.941137f, 0.068893f),
    floatArrayOf(-0.103204f, -0.938736f, 0.120865f),
    floatArrayOf(-0.134494f, -0.947649f, 0.117231f),
    floatArrayOf(-0.181033f, -0.964318f, 0.106760f),
    floatArrayOf(-0.223351f, -0.956648f, 0.051735f),
    floatArrayOf(-0.225932f, -0.933921f, 0.079522f),
    floatArrayOf(-0.205498f, -0.889097f, 0.116805f),
    floatArrayOf(-0.185304f, -0.916649f, 0.166660f),
    floatArrayOf(-0.224562f, -0.915146f, 0.152490f),
    floatArrayOf(-0.247480f, -0.915985f, 0.159062f),
    floatArrayOf(-0.254363f, -0.907394f, 0.142650f),
    floatArrayOf(-0.260789f, -0.927790f, 0.156991f),
    floatArrayOf(-0.266296f, -0.921235f, 0.143667f),
    floatArrayOf(-0.262978f, -0.916518f, 0.142166f),
    floatArrayOf(-0.254073f, -0.897515f, 0.136966f),
    floatArrayOf(-0.251796f, -0.893623f, 0.159510f),
    floatArrayOf(-0.276890f, -0.915159f, 0.150614f),
    floatArrayOf(-0.299247f, -0.919136f, 0.132336f),
    floatArrayOf(-0.325069f, -0.871478f, 0.081651f),
    floatArrayOf(-0.335923f, -0.857466f, 0.132109f),
    floatArrayOf(-0.334260f, -0.838275f, 0.178194f),
    floatArrayOf(-0.333070f, -0.878679f, 0.224480f),
    floatArrayOf(-0.331844f, -0.924307f, 0.125887f),
    floatArrayOf(-0.312300f, -0.877397f, 0.101939f),
    floatArrayOf(-0.328471f, -0.757474f, 0.113005f),
    floatArrayOf(-0.315449f, -0.750315f, 0.187775f),
    floatArrayOf(-0.352767f, -0.745971f, 0.150969f),
    floatArrayOf(-0.384764f, -0.742200f, 0.131212f),
    floatArrayOf(-0.363180f, -0.704325f, 0.067940f),
    floatArrayOf(-0.315235f, -0.670305f, 0.071777f),
    floatArrayOf(-0.296822f, -0.587724f, 0.043577f),
    floatArrayOf(-0.427707f, -0.503986f, 0.071056f),
    floatArrayOf(-0.550169f, -0.413848f, -0.034389f),
    floatArrayOf(-0.628431f, -0.425833f, 0.052434f),
    floatArrayOf(-0.721198f, -0.346203f, 0.096508f),
    floatArrayOf(-0.625616f, -0.215951f, 0.086614f),
    floatArrayOf(-0.397413f, -0.038036f, -0.133171f),
    floatArrayOf(-0.436804f, 0.001031f, -0.086545f),
    floatArrayOf(-0.517375f, 0.139519f, -0.233589f),
    floatArrayOf(-0.533258f, 0.109334f, -0.076022f),
    floatArrayOf(-0.522659f, 0.019306f, 0.247374f),
    floatArrayOf(-0.301295f, -0.092150f, 0.485914f),
    floatArrayOf(-0.462715f, 0.147457f, -0.667510f),
    floatArrayOf(-0.677845f, -0.289889f, -0.497773f),
    floatArrayOf(-0.927320f, -0.548739f, 0.155318f),
    floatArrayOf(-1.355252f, 0.118646f, 0.233433f),
    floatArrayOf(-1.925418f, 0.171447f, 0.030200f),
    floatArrayOf(-2.693436f, -0.306908f, -0.119710f),
    floatArrayOf(-4.401943f, -0.249540f, 0.397466f),
    floatArrayOf(-3.417717f, -0.206146f, 0.398775f),
    floatArrayOf(-1.683946f, -0.012039f, 0.692873f),
    floatArrayOf(-1.168271f, 0.040200f, 0.440300f),
    floatArrayOf(-0.879689f, -0.374715f, 0.020136f),
    floatArrayOf(-1.122831f, -0.126452f, 0.320121f),
    floatArrayOf(-0.945366f, -0.041351f, 0.563179f),
    floatArrayOf(-0.427332f, 0.049551f, 0.519074f),
    floatArrayOf(-0.261967f, 0.157023f, 0.520631f),
    floatArrayOf(-0.022099f, 0.319514f, 0.147569f),
    floatArrayOf(-0.160723f, 0.409034f, 0.285545f),
    floatArrayOf(-0.687852f, 0.492607f, 0.328380f),
    floatArrayOf(-1.032613f, 0.546785f, 0.410982f),
    floatArrayOf(-1.007273f, 0.581848f, 0.269807f),
    floatArrayOf(-0.673244f, 0.386014f, 0.221661f),
    floatArrayOf(-0.649982f, 0.300118f, 0.252834f),
    floatArrayOf(-0.961106f, 0.430313f, 0.458135f),
    floatArrayOf(-1.242120f, 0.573851f, 0.604007f),
    floatArrayOf(-1.224695f, 0.581751f, 0.651613f),
    floatArrayOf(-1.457415f, 0.350976f, 0.688528f),
    floatArrayOf(-1.256448f, 0.243852f, 0.446888f),
    floatArrayOf(-0.871408f, 0.100915f, 0.316803f),
    floatArrayOf(-0.624149f, -0.117225f, 0.496180f),
    floatArrayOf(-0.520036f, -0.030365f, 0.539917f),
    floatArrayOf(-0.538147f, 0.133185f, 0.676295f),
    floatArrayOf(-0.628895f, 0.111283f, 0.628562f),
    floatArrayOf(-0.618413f, -0.047875f, 0.570504f),
    floatArrayOf(-0.685540f, -0.234613f, 0.509401f),
    floatArrayOf(-0.749321f, -0.283698f, 0.507021f),
    floatArrayOf(-0.742609f, -0.185157f, 0.450456f),
    floatArrayOf(-0.661776f, -0.150940f, 0.578822f),
    floatArrayOf(-0.924465f, -0.092981f, 0.524019f),
    floatArrayOf(-0.986956f, -0.251130f, 0.430777f),
    floatArrayOf(-0.701161f, -0.414475f, 0.535951f),
    floatArrayOf(-0.615489f, -0.264414f, 0.543475f),
    floatArrayOf(-0.746839f, -0.037632f, 0.457952f),
    floatArrayOf(-0.925180f, 0.006251f, 0.509027f),
    floatArrayOf(-0.985187f, -0.018267f, 0.493911f),
    floatArrayOf(-0.988793f, 0.013489f, 0.396998f),
    floatArrayOf(-0.962855f, 0.045184f, 0.297628f),
    floatArrayOf(-0.973606f, 0.023746f, 0.302750f),
    floatArrayOf(-0.997493f, 0.028719f, 0.289303f),
    floatArrayOf(-1.035242f, 0.060215f, 0.309469f),
    floatArrayOf(-1.055979f, 0.088078f, 0.262475f),
    floatArrayOf(-1.050408f, 0.090342f, 0.248870f),
    floatArrayOf(-1.058908f, 0.119139f, 0.237851f),
    floatArrayOf(-1.014153f, 0.097449f, 0.264318f),
    floatArrayOf(-0.968511f, 0.046678f, 0.249582f),
    floatArrayOf(-0.926817f, -0.011237f, 0.256903f),
    floatArrayOf(-0.922213f, -0.005508f, 0.239174f)
)

val realDropWindow = arrayOf(
    floatArrayOf(-0.042907f, -0.018932f, 1.011411f),
    floatArrayOf(0.028161f, -0.006546f, 0.989066f),
    floatArrayOf(-0.035389f, 0.026612f, 0.979596f),
    floatArrayOf(0.004322f, 0.017718f, 0.974940f),
    floatArrayOf(0.007313f, 0.008489f, 0.996941f),
    floatArrayOf(0.000244f, 0.010389f, 0.993132f),
    floatArrayOf(0.012283f, -0.005745f, 1.009371f),
    floatArrayOf(-0.007282f, -0.018002f, 0.988437f),
    floatArrayOf(0.024950f, 0.021480f, 1.033556f),
    floatArrayOf(-0.002682f, -0.005807f, 1.005499f),
    floatArrayOf(-0.007343f, -0.003039f, 1.011393f),
    floatArrayOf(-0.003641f, 0.001721f, 0.993790f),
    floatArrayOf(0.022676f, 0.001687f, 1.022507f),
    floatArrayOf(-0.019157f, 0.005783f, 1.046325f),
    floatArrayOf(-0.003581f, -0.022945f, 1.008299f),
    floatArrayOf(-0.003755f, 0.021227f, 1.018868f),
    floatArrayOf(-0.004299f, 0.009430f, 1.003797f),
    floatArrayOf(-0.030025f, -0.030554f, 0.975126f),
    floatArrayOf(-0.015512f, 0.008382f, 1.010964f),
    floatArrayOf(0.025067f, 0.016295f, 1.007836f),
    floatArrayOf(-0.010455f, -0.008329f, 1.019287f),
    floatArrayOf(-0.001682f, -0.016738f, 0.962296f),
    floatArrayOf(-0.029253f, 0.006644f, 0.998397f),
    floatArrayOf(-0.005458f, -0.014309f, 0.975204f),
    floatArrayOf(0.012011f, -0.032849f, 1.036463f),
    floatArrayOf(-0.007957f, 0.028664f, 0.974478f),
    floatArrayOf(0.012975f, 0.033360f, 1.065041f),
    floatArrayOf(-0.009093f, -0.026993f, 0.989980f),
    floatArrayOf(0.017242f, -0.028263f, 0.966984f),
    floatArrayOf(-0.041937f, -0.019496f, 0.992189f),
    floatArrayOf(0.006723f, 0.062843f, 0.991708f),
    floatArrayOf(0.020982f, 0.035133f, 1.020554f),
    floatArrayOf(0.039241f, 0.028155f, 0.960381f),
    floatArrayOf(-0.046206f, 0.036126f, 1.002188f),
    floatArrayOf(0.016899f, -0.022563f, 0.993225f),
    floatArrayOf(0.008987f, 0.028667f, 0.981099f),
    floatArrayOf(0.005069f, -0.010466f, 1.004801f),
    floatArrayOf(0.041980f, 0.008157f, 1.018057f),
    floatArrayOf(0.031862f, 0.020140f, 1.029225f),
    floatArrayOf(0.023807f, 0.008538f, 0.983283f),
    floatArrayOf(0.009884f, -0.000746f, 1.000586f),
    floatArrayOf(0.016187f, 0.003121f, 1.035476f),
    floatArrayOf(1.122079f, 0.436742f, 0.274887f),
    floatArrayOf(-1.828523f, 0.373126f, -0.606154f),
    floatArrayOf(-0.673722f, 0.753544f, 0.320491f),
    floatArrayOf(1.142535f, 1.202047f, 1.007155f),
    floatArrayOf(-0.120110f, 1.518728f, -0.466103f),
    floatArrayOf(-0.812891f, 2.109464f, -0.757695f),
    floatArrayOf(-0.408094f, 3.067363f, 0.044142f),
    floatArrayOf(0.091847f, 5.040990f, -0.283040f),
    floatArrayOf(0.286047f, 3.931297f, -0.166874f),
    floatArrayOf(1.013858f, 1.991893f, -0.123634f),
    floatArrayOf(0.522962f, 1.362949f, -0.200067f),
    floatArrayOf(0.529513f, 1.082411f, 0.610376f),
    floatArrayOf(0.608395f, 1.336579f, 0.125023f),
    floatArrayOf(1.004833f, 1.154654f, 0.055160f),
    floatArrayOf(0.879115f, 0.545858f, -0.040408f),
    floatArrayOf(0.015530f, -0.020056f, 0.987991f),
    floatArrayOf(-0.004366f, 0.014431f, 1.056252f),
    floatArrayOf(-0.003491f, -0.016905f, 0.986874f),
    floatArrayOf(-0.025436f, 0.003268f, 1.009085f),
    floatArrayOf(0.003214f, 0.014629f, 1.028367f),
    floatArrayOf(0.009463f, -0.034809f, 0.974415f),
    floatArrayOf(0.001311f, -0.013452f, 1.052566f),
    floatArrayOf(0.001381f, -0.002540f, 1.005612f),
    floatArrayOf(0.021442f, 0.006498f, 1.018753f),
    floatArrayOf(-0.017723f, 0.006086f, 0.998974f),
    floatArrayOf(-0.001474f, 0.023186f, 0.986274f),
    floatArrayOf(0.004826f, -0.013423f, 0.972452f),
    floatArrayOf(0.002069f, -0.011552f, 1.011312f),
    floatArrayOf(-0.012535f, -0.014135f, 0.994603f),
    floatArrayOf(0.025714f, 0.013533f, 1.010416f),
    floatArrayOf(0.018763f, -0.007140f, 0.986850f),
    floatArrayOf(0.036846f, -0.011398f, 1.002108f),
    floatArrayOf(-0.007658f, 0.023308f, 0.982015f),
    floatArrayOf(0.017033f, 0.020091f, 0.997910f),
    floatArrayOf(0.004380f, -0.017860f, 1.001262f),
    floatArrayOf(-0.004219f, -0.020769f, 1.008728f),
    floatArrayOf(-0.012478f, -0.031192f, 0.984002f),
    floatArrayOf(-0.009200f, 0.004482f, 1.023290f),
    floatArrayOf(0.012418f, 0.028001f, 0.988181f),
    floatArrayOf(-0.018911f, -0.014757f, 1.007848f),
    floatArrayOf(0.000029f, -0.009330f, 1.006833f),
    floatArrayOf(0.028806f, -0.007480f, 0.967500f),
    floatArrayOf(-0.006248f, -0.002893f, 1.005053f),
    floatArrayOf(0.007305f, 0.027611f, 1.008220f),
    floatArrayOf(0.010874f, 0.022745f, 1.000853f),
    floatArrayOf(-0.008592f, 0.024656f, 0.998218f),
    floatArrayOf(0.020966f, -0.022920f, 0.977464f),
    floatArrayOf(-0.004445f, -0.017756f, 0.992490f),
    floatArrayOf(0.000430f, -0.002474f, 0.974396f),
    floatArrayOf(-0.017547f, -0.010801f, 1.007899f),
    floatArrayOf(-0.000369f, 0.010095f, 0.986826f),
    floatArrayOf(0.016784f, -0.022853f, 0.997305f),
    floatArrayOf(-0.032332f, -0.041350f, 0.988648f),
    floatArrayOf(0.004872f, 0.021127f, 1.002468f),
    floatArrayOf(0.044487f, 0.005135f, 1.013888f),
    floatArrayOf(-0.003559f, -0.018153f, 0.992351f),
    floatArrayOf(-0.001072f, 0.036120f, 0.990203f),
    floatArrayOf(-0.019974f, 0.015549f, 1.033295f)
)

val realAdlWindow = arrayOf(
    floatArrayOf(-0.075563f, -0.038774f, 0.973834f),
    floatArrayOf(-0.075273f, -0.052117f, 0.976718f),
    floatArrayOf(-0.073132f, -0.048934f, 0.976601f),
    floatArrayOf(-0.079026f, -0.046089f, 0.968051f),
    floatArrayOf(-0.075433f, -0.042098f, 0.969058f),
    floatArrayOf(-0.076025f, -0.047875f, 0.966825f),
    floatArrayOf(-0.074770f, -0.049804f, 0.977498f),
    floatArrayOf(-0.071171f, -0.050919f, 0.970834f),
    floatArrayOf(-0.076803f, -0.040107f, 0.972142f),
    floatArrayOf(-0.074094f, -0.046160f, 0.976158f),
    floatArrayOf(-0.073826f, -0.048299f, 0.973982f),
    floatArrayOf(-0.074064f, -0.045145f, 0.973795f),
    floatArrayOf(-0.079865f, -0.049467f, 0.975393f),
    floatArrayOf(-0.074408f, -0.045518f, 0.974600f),
    floatArrayOf(-0.075134f, -0.049458f, 0.972589f),
    floatArrayOf(-0.068582f, -0.057382f, 0.969630f),
    floatArrayOf(-0.075410f, -0.041519f, 0.978151f),
    floatArrayOf(-0.075319f, -0.045306f, 0.976202f),
    floatArrayOf(-0.075811f, -0.045510f, 0.981951f),
    floatArrayOf(-0.077654f, -0.042303f, 0.972406f),
    floatArrayOf(-0.071451f, -0.049563f, 0.975524f),
    floatArrayOf(-0.068621f, -0.043872f, 0.972555f),
    floatArrayOf(-0.070123f, -0.045792f, 0.976504f),
    floatArrayOf(-0.068304f, -0.038696f, 0.977868f),
    floatArrayOf(-0.069334f, -0.043602f, 0.979142f),
    floatArrayOf(-0.070472f, -0.046214f, 0.970624f),
    floatArrayOf(-0.077705f, -0.043428f, 0.964740f),
    floatArrayOf(-0.073946f, -0.053047f, 0.979684f),
    floatArrayOf(-0.077825f, -0.040111f, 0.974479f),
    floatArrayOf(-0.072660f, -0.032178f, 0.970020f),
    floatArrayOf(-0.071589f, -0.034166f, 0.976369f),
    floatArrayOf(-0.062800f, -0.039547f, 0.975651f),
    floatArrayOf(-0.070096f, -0.046183f, 0.976810f),
    floatArrayOf(-0.069233f, -0.038610f, 0.976543f),
    floatArrayOf(-0.071996f, -0.036944f, 0.975351f),
    floatArrayOf(-0.063667f, -0.037219f, 0.973631f),
    floatArrayOf(-0.068702f, -0.032874f, 0.968477f),
    floatArrayOf(-0.077382f, -0.034090f, 0.976185f),
    floatArrayOf(-0.070202f, -0.036508f, 0.974620f),
    floatArrayOf(-0.068985f, -0.039086f, 0.974144f),
    floatArrayOf(-0.069326f, -0.037457f, 0.977353f),
    floatArrayOf(-0.067764f, -0.037003f, 0.974943f),
    floatArrayOf(-0.064592f, -0.029823f, 0.980333f),
    floatArrayOf(-0.066765f, -0.029478f, 0.984503f),
    floatArrayOf(-0.067096f, -0.043873f, 0.971576f),
    floatArrayOf(-0.066599f, -0.034569f, 0.975465f),
    floatArrayOf(-0.075002f, -0.030765f, 0.980039f),
    floatArrayOf(-0.072683f, -0.033035f, 0.988022f),
    floatArrayOf(-0.072306f, -0.029068f, 0.974361f),
    floatArrayOf(-0.073285f, -0.032426f, 0.987264f),
    floatArrayOf(-0.068446f, -0.034808f, 0.971087f),
    floatArrayOf(-0.070155f, -0.032226f, 0.980485f),
    floatArrayOf(-0.069640f, -0.021859f, 0.974812f),
    floatArrayOf(-0.074388f, -0.024242f, 0.978593f),
    floatArrayOf(-0.072668f, -0.025091f, 0.983218f),
    floatArrayOf(-0.070019f, -0.027681f, 0.980385f),
    floatArrayOf(-0.069569f, -0.024671f, 0.979447f),
    floatArrayOf(-0.069278f, -0.025125f, 0.976915f),
    floatArrayOf(-0.067827f, -0.029818f, 0.971474f),
    floatArrayOf(-0.066236f, -0.027163f, 0.973174f),
    floatArrayOf(-0.069760f, -0.016917f, 0.977922f),
    floatArrayOf(-0.067234f, -0.022719f, 0.980167f),
    floatArrayOf(-0.066008f, -0.025885f, 0.984998f),
    floatArrayOf(-0.069019f, -0.030321f, 0.973858f),
    floatArrayOf(-0.075822f, -0.016398f, 0.970652f),
    floatArrayOf(-0.069627f, -0.016176f, 0.971055f),
    floatArrayOf(-0.066948f, -0.021160f, 0.976192f),
    floatArrayOf(-0.069448f, -0.015996f, 0.968551f),
    floatArrayOf(-0.070997f, -0.019486f, 0.986433f),
    floatArrayOf(-0.069501f, -0.022391f, 0.974749f),
    floatArrayOf(-0.069942f, -0.017957f, 0.977370f),
    floatArrayOf(-0.073184f, -0.012935f, 0.979061f),
    floatArrayOf(-0.074973f, -0.022080f, 0.979110f),
    floatArrayOf(-0.071483f, -0.015421f, 0.977773f),
    floatArrayOf(-0.068943f, -0.025560f, 0.977283f),
    floatArrayOf(-0.065493f, -0.016065f, 0.984126f),
    floatArrayOf(-0.062426f, -0.019499f, 0.981694f),
    floatArrayOf(-0.061045f, -0.025714f, 0.981955f),
    floatArrayOf(-0.065526f, -0.025255f, 0.976434f),
    floatArrayOf(-0.070066f, -0.022168f, 0.975315f),
    floatArrayOf(-0.072391f, -0.023143f, 0.970207f),
    floatArrayOf(-0.065555f, -0.023490f, 0.970776f),
    floatArrayOf(-0.070619f, -0.021306f, 0.971085f),
    floatArrayOf(-0.064370f, -0.017446f, 0.978043f),
    floatArrayOf(-0.068956f, -0.014966f, 0.985582f),
    floatArrayOf(-0.066426f, -0.008512f, 0.982709f),
    floatArrayOf(-0.065928f, -0.022332f, 0.978196f),
    floatArrayOf(-0.067316f, -0.013063f, 0.980748f),
    floatArrayOf(-0.074080f, -0.023935f, 0.975800f),
    floatArrayOf(-0.068908f, -0.022542f, 0.984226f),
    floatArrayOf(-0.072123f, -0.018545f, 0.988017f),
    floatArrayOf(-0.066857f, -0.028108f, 1.011462f),
    floatArrayOf(-0.068611f, -0.012573f, 1.007502f),
    floatArrayOf(-0.071185f, -0.008456f, 1.024132f),
    floatArrayOf(-0.072802f, -0.021914f, 1.045236f),
    floatArrayOf(-0.067801f, -0.056663f, 1.021991f),
    floatArrayOf(-0.055594f, -0.079667f, 0.976280f),
    floatArrayOf(-0.061076f, -0.071082f, 0.942821f),
    floatArrayOf(-0.036727f, -0.043498f, 0.948644f),
    floatArrayOf(-0.037290f, -0.016094f, 0.976827f)
)


class SosForegroundService : Service(), SensorEventListener {

    companion object {
        const val ACTION_MANUAL_SOS = "ACTION_MANUAL_SOS"
        const val ACTION_SIMULATE_FALL = "ACTION_SIMULATE_FALL"

        private const val SHAKE_THRESHOLD_GFORCE = 35.0f // separate (lower/simpler) threshold than the ML fall model
        private const val SHAKE_WINDOW_MS = 1500L
        private const val SHAKE_COUNT_NEEDED = 4

        private const val POWER_PRESS_WINDOW_MS = 2500L
        private const val POWER_PRESS_COUNT_NEEDED = 3
        private var isFallDetectionEnabled = false
        private var shakeTriggerEnabled = false
        private val shakeTimestamps = mutableListOf<Long>()
        private var lastShakeSosTime = 0L
        const val ACTION_START_CONTINUOUS_VOICE_MONITORING = "com.myapplication.app.START_CONTINUOUS_VOICE"
        const val ACTION_STOP_CONTINUOUS_VOICE_MONITORING = "com.myapplication.app.STOP_CONTINUOUS_VOICE"
        const val ACTION_POST_FALL_MONITORING_ONLY = "com.myapplication.app.ACTION_POST_FALL_MONITORING_ONLY"
        const val ACTION_STOP_SERVICE = "com.myapplication.app.ACTION_STOP_SERVICE"

        private const val NOTIFICATION_ID = 1
        const val NOTIFICATION_ID_FALL_DETECTION = 2
        const val NOTIFICATION_ID_POST_FALL_VOICE_MONITORING = 3
        const val NOTIFICATION_ID_CONTINUOUS_VOICE_MONITORING = 4
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val channelId = "SOS_SERVICE_CHANNEL"

    private lateinit var fallModel: FallDetectionModel

    // Sliding window buffer for the ML fall model
    private val windowSize = 100   // number of samples per window
    private val buffer = mutableListOf<FloatArray>()

    // shake trigger state
    private var shakeTriggerEnabled = false
    private val shakeTimestamps = mutableListOf<Long>()
    private var lastShakeSosTime = 0L

    // power button (screen off/on) trigger state
    private var powerButtonTriggerEnabled = false
    private val screenOffTimestamps = mutableListOf<Long>()
    private var screenReceiverRegistered = false

    //For Voice Monitoring for SOS Keywords
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager
    private var sensorRegistered = false
    private var pause = false
    private var postFallPause = false


    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!powerButtonTriggerEnabled) return
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                val now = System.currentTimeMillis()
                screenOffTimestamps.add(now)
                screenOffTimestamps.removeAll { (now - it) > POWER_PRESS_WINDOW_MS }
                if (screenOffTimestamps.size >= POWER_PRESS_COUNT_NEEDED) {
                    screenOffTimestamps.clear()
                    triggerPowerButtonSos()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        fallModel = FallDetectionModel(this)

        voiceRecognitionManager = VoiceRecognitionManager (context = applicationContext, onTextRecognized = {
            text -> handleVoiceText(text) },
            errorState = {
                error-> Log.e("VOSK", error)
            }
        )
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MANUAL_SOS -> {
                triggerManualSos()
                return START_STICKY
            }

            ACTION_SIMULATE_FALL -> {
                simulateFallEvent()
                return START_STICKY
            }

            ACTION_START_CONTINUOUS_VOICE_MONITORING -> {
                voiceRecognitionManager.isPostFallMonitoringActive = false
                voiceRecognitionManager.startContinuousVoiceMonitoring()
                val manager = getSystemService(NotificationManager::class.java)
                manager.cancel(NOTIFICATION_ID_POST_FALL_VOICE_MONITORING)
            }

            ACTION_STOP_CONTINUOUS_VOICE_MONITORING -> {
                voiceRecognitionManager.stopContinuousVoiceMonitoring()
            }

            ACTION_POST_FALL_MONITORING_ONLY -> {
                voiceRecognitionManager.stopContinuousVoiceMonitoring()
                val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                if (prefs.getBoolean("isFallDetectionRunning", false)) {
                    displayPostFallMonitoringNotification()
                }
            }

            ACTION_STOP_SERVICE -> {
                val manager = getSystemService(NotificationManager::class.java)
                manager.cancel(NOTIFICATION_ID_POST_FALL_VOICE_MONITORING)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

        }

        // Start the foreground notification shield
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        val isFallDetectionEnabled = intent?.getBooleanExtra("ENABLE_FALL_DETECTION", false) ?: false
        shakeTriggerEnabled = intent?.getBooleanExtra("ENABLE_SHAKE_TRIGGER", false) ?: false
        powerButtonTriggerEnabled = intent?.getBooleanExtra("ENABLE_POWER_BUTTON_TRIGGER", false) ?: false

        // register/unregister accelerometer if either fall detection or shake trigger needs it
        if (isFallDetectionEnabled || shakeTriggerEnabled) {
            accelerometer?.let { sensor ->
                if (!sensorRegistered) {
                    sensorManager.registerListener(this, sensor, 20_000) // 50 Hz fixed
                }
            }
        } else {
            if (sensorRegistered) {
                sensorManager.unregisterListener(this)
            }
        }

        // register/unregister the screen state receiver for the power-button trigger
        if (powerButtonTriggerEnabled && !screenReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenStateReceiver, filter)
            screenReceiverRegistered = true
        } else if (!powerButtonTriggerEnabled && screenReceiverRegistered) {
            try { unregisterReceiver(screenStateReceiver) } catch (e: Exception) { e.printStackTrace() }
            screenReceiverRegistered = false
        }

        return START_STICKY
    }


    private fun simulateFallEvent() {

val window=realFallWindow

        val prediction = fallModel.predict(window)
        val classNames = arrayOf("ADL", "Fall", "Phone Drop")
        val result = classNames[prediction]

        android.util.Log.d("MLTest", "Simulated Prediction: $result")
        Toast.makeText(this, "Simulated fall triggered", Toast.LENGTH_SHORT).show()

        if (result == "Fall") {
            triggerSOS()
            voiceRecognitionManager.monitorVoiceAfterFallDetected()
        }
    }



    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Shake-to-SOS gesture — only runs when explicitly toggled on.
            if (shakeTriggerEnabled) {
                val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                checkForShake(gForce)
            }

            // ML fall detection — only predicts when explicitly enabled,
            // so enabling shake-trigger alone can never silently also run this.
            if (isFallDetectionEnabled) {
                val xg = x / SensorManager.GRAVITY_EARTH
                val yg = y / SensorManager.GRAVITY_EARTH
                val zg = z / SensorManager.GRAVITY_EARTH

                buffer.add(floatArrayOf(xg, yg, zg))

                if (buffer.size >= windowSize) {
                    val window = buffer.toTypedArray()
                    buffer.clear()

                    val prediction = fallModel.predict(window)
                    val classNames = arrayOf("ADL", "Fall", "Phone Drop")
                    val result = classNames[prediction]

                    if (result == "Fall") {
                        triggerSOS()
                        voiceRecognitionManager.monitorVoiceAfterFallDetected()
                    }
                }
            } else if (buffer.isNotEmpty()) {
                buffer.clear() // it help in not  letting stale samples linger if fall detection gets toggled off mid-window
            }
        }
    }
    private fun checkForShake(gForce: Float) {
        if (gForce < SHAKE_THRESHOLD_GFORCE) return
        val now = System.currentTimeMillis()

        // cooldown so one shake gesture doesn't refire the SOS ten times in a row
        if (now - lastShakeSosTime < 5000L) return

        shakeTimestamps.add(now)
        shakeTimestamps.removeAll { now - it > SHAKE_WINDOW_MS }

        if (shakeTimestamps.size >= SHAKE_COUNT_NEEDED) {
            shakeTimestamps.clear()
            lastShakeSosTime = now
            triggerShakeSos()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerManualSos() {
        val sosIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("MANUAL_SOS_TRIGGERED", true)
        }
        startActivity(sosIntent)
    }

    private fun triggerSOS() {
        val wakeIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("FALL_DETECTED", true)
        }
        startActivity(wakeIntent)
    }

    private fun triggerShakeSos() {
        val shakeIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("SHAKE_SOS_TRIGGERED", true)
        }
        startActivity(shakeIntent)
    }

    private fun triggerPowerButtonSos() {
        val powerIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("POWER_BUTTON_SOS_TRIGGERED", true)
        }
        startActivity(powerIntent)
    }

    private fun triggerVoiceAssistedSOS(text: String) {
        val sosIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("VOICE_SOS_TRIGGERED", true)
            putExtra("SOS_REASON", "Voice Assisted SOS Alarm")
            putExtra("SOS_KEYWORD_TEXT", text)
        }
        startActivity(sosIntent)
    }

    private fun cancelAutomaticSOS() {
        val cancelIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("VOICE_SOS_CANCELLED", true)
        }
        startActivity(cancelIntent)
    }

    private fun handleVoiceText(text: String) {
        val detector = KeywordDetector()
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val alwaysListen = prefs.getBoolean("listeningForKeyword", false)


        if (alwaysListen) {

            // MONITORS FOR SOS KEYWORDS ONLY DURING 24/7 CONTINUOUS VOICE ASSISTANCE
            if (detector.containsSOSKeyword(text) && !pause ){
                pause = true
                Toast.makeText(this, "Detected: \"$text\". Voice Assisted SOS Triggered!", Toast.LENGTH_LONG).show()
                triggerVoiceAssistedSOS(text)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    pause = false },
                    5000L
                )
            }

            return  //Ignores Alright Keywords in 24/7 Continuous Voice Monitoring
        }

        if (!voiceRecognitionManager.isPostFallMonitoringActive) {
            return
        }




        // MONITORS FOR BOTH SOS AND ALRIGHT KEYWORDS AFTER A FALL IS DETECTED
        if (detector.containsSOSKeyword(text) && !postFallPause) {
            postFallPause = true
            Toast.makeText(this, "Detected: \"$text\". Voice Assisted SOS Triggered!", Toast.LENGTH_LONG).show()

            triggerVoiceAssistedSOS(text)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                pause = false
            }, 5000L)

        }

        if (detector.containsAlrightKeyword(text)) {
            cancelAutomaticSOS()
            Toast.makeText(this, "Detected: \"$text\". Voice Assisted SOS Cancelled!", Toast.LENGTH_LONG).show()
            voiceRecognitionManager.stopPostFallMonitoring()

        }
    }
    private fun buildNotification(): Notification {

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val fallDetection = prefs.getBoolean("isFallDetectionRunning", false)
        val voiceAssistance = prefs.getBoolean("listeningForKeyword", false)

        val title: String
        val text: String
        val ongoing : Boolean

        when {
            fallDetection && voiceAssistance -> {
                title = "Two-Way Alert Active"
                text = "Fall Detection and Continuous Voice Assistance are active."
                ongoing = true
            }

            voiceAssistance -> {
                title = "Continuous Voice Assistance Active"
                text = "Continuously monitoring for SOS keywords in the background."
                ongoing = true
            }

            fallDetection -> {
                title = "24/7 Fall Detection Active"
                text = "Monitoring for falls in the background."
                ongoing = true

            }

            else -> {
                title = "Two-Way Alert Active"
                text = "Background monitoring is active."
                ongoing = false
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Manual SOS button
        val sosIntent = Intent(this, SosForegroundService::class.java).apply { action = ACTION_MANUAL_SOS }
        val sosPendingIntent = PendingIntent.getService(
            this, 1, sosIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Simulate Fall button
        val simulateIntent = Intent(this, SosForegroundService::class.java).apply { action = ACTION_SIMULATE_FALL }
        val simulatePendingIntent = PendingIntent.getService(
            this, 2, simulateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "SEND SOS NOW", sosPendingIntent)
            .addAction(android.R.drawable.ic_menu_info_details, "Simulate Fall", simulatePendingIntent)
            .setOngoing(ongoing)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SOS Background Service", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun displayPostFallMonitoringNotification() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("POST FALL VOICE MONITORING READY")
            .setContentText("Voice assistance will activate automatically when a fall is detected.")
            .setOngoing(false)
            .setAutoCancel(false)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_POST_FALL_VOICE_MONITORING, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        fallModel.close()
        voiceRecognitionManager.release()
        sensorRegistered = false
        if (screenReceiverRegistered) {
            try { unregisterReceiver(screenStateReceiver) } catch (e: Exception) { e.printStackTrace() }
            screenReceiverRegistered = false
        }
    }
}




