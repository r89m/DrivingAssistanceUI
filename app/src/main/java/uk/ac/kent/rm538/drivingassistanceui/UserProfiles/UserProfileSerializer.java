package uk.ac.kent.rm538.drivingassistanceui.UserProfiles;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


import java.lang.reflect.Type;

import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.DefaultFeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.Feedback.FeedbackConfiguration.FeedbackConfiguration;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.SimpleTimeTrigger;
import uk.ac.kent.rm538.drivingassistanceui.TimeTrigger.TimeTrigger;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.UserLocation;
import uk.ac.kent.rm538.drivingassistanceui.UserLocation.UserLocationImpl;

/**
 * Created by Richard on 26/01/2016.
 */
public class UserProfileSerializer implements JsonSerializer<UserProfile>, JsonDeserializer<UserProfile> {

    private static final String JSON_FIELD_NAME = "name";
    private static final String JSON_FIELD_SENSITIVITY_FORWARD = "sensitivity_forward";
    private static final String JSON_FIELD_SENSITIVITY_BACKWARD = "sensitivity_backward";
    private static final String JSON_FIELD_SENSITIVITY_SIDEWAYS = "sensitivity_sideways";
    private static final String JSON_FIELD_ACTIVATION_LOCATION = "activation_location";
    private static final String JSON_FIELD_TIME_TRIGGER = "time_trigger";
    private static final String JSON_FIELD_FEEDBACK_CONFIGURATION = "feedback_configuration";

    @Override
    public JsonElement serialize(UserProfile userProfile, Type typeOfSrc, JsonSerializationContext context) {

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(JSON_FIELD_NAME, userProfile.getName());
        jsonObject.addProperty(JSON_FIELD_SENSITIVITY_FORWARD, userProfile.getForwardSensitivity());
        jsonObject.addProperty(JSON_FIELD_SENSITIVITY_BACKWARD, userProfile.getBackwardSensitivity());
        jsonObject.addProperty(JSON_FIELD_SENSITIVITY_SIDEWAYS, userProfile.getSidewaysSensitivity());
        jsonObject.add(JSON_FIELD_ACTIVATION_LOCATION, context.serialize(userProfile.getActivationLocation()));
        jsonObject.add(JSON_FIELD_TIME_TRIGGER, context.serialize(userProfile.getTimeTrigger()));
        jsonObject.add(JSON_FIELD_FEEDBACK_CONFIGURATION, context.serialize(userProfile.getFeedbackConfiguration()));
        return jsonObject;
    }

    @Override
    public UserProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        final JsonObject jsonObject = json.getAsJsonObject();

        String name = jsonObject.get(JSON_FIELD_NAME).getAsString();
        int forwardSensitivity = jsonObject.get(JSON_FIELD_SENSITIVITY_FORWARD).getAsInt();
        int backwardSensitivity = jsonObject.get(JSON_FIELD_SENSITIVITY_BACKWARD).getAsInt();
        int sidewaysSensitivity = jsonObject.get(JSON_FIELD_SENSITIVITY_SIDEWAYS).getAsInt();

        FeedbackConfiguration feedbackConfiguration = context.deserialize(jsonObject.get(JSON_FIELD_FEEDBACK_CONFIGURATION), DefaultFeedbackConfiguration.class);
        UserLocation userLocation = context.deserialize(jsonObject.get(JSON_FIELD_ACTIVATION_LOCATION), UserLocationImpl.class);
        TimeTrigger timeTrigger = context.deserialize(jsonObject.get(JSON_FIELD_TIME_TRIGGER), SimpleTimeTrigger.class);

        final UserProfileImpl userProfile = new UserProfileImpl(name);
        userProfile.setWheelchairSensitivity(forwardSensitivity, backwardSensitivity, sidewaysSensitivity);
        userProfile.setActivationLocation(userLocation);
        userProfile.setTimeTrigger(timeTrigger);
        userProfile.setFeedbackConfiguration(feedbackConfiguration);
        return userProfile;
    }
}
