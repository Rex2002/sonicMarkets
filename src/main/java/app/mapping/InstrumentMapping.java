package app.mapping;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import audio.synth.InstrumentEnum;
import dataRepo.SonifiableID;

public class InstrumentMapping {
	// Optional fields represent that the user can leave those empty
	// Any other fields have to be set for a valid mapping
	// They can still be null, if the mapping isn't finished
	// Fields are set to public, so methods like `isEmpty` can use introspection
	public final InstrumentEnum instrument;
	// Pitch
	public ExchangeData<LineData> pitch = null;
	// Volume
	public ExchangeData<LineData> relVolume = null;
	public ExchangeData<RangeData> absVolume = null;
	// Echo
	public ExchangeData<LineData> delayEcho = null;
	public ExchangeData<LineData> feedbackEcho = null;
	public ExchangeData<RangeData> onOffEcho = null;
	// Reverb parameters
	public ExchangeData<LineData> delayReverb = null;
	public ExchangeData<LineData> feedbackReverb = null;
	public ExchangeData<RangeData> onOffReverb = null;
	// Filter parameters
	public ExchangeData<LineData> cutoff = null;
	public ExchangeData<RangeData> onOffFilter = null;
	public boolean highPass = false;
	// Panning
	public ExchangeData<LineData> pan = null;

	InstrumentMapping(InstrumentEnum instrument) {
		this.instrument = instrument;
	}

	public InstrParam[] getEmptyLineParams(InstrParam oldVal) {
		List<InstrParam> params = new ArrayList<>();
		if (pitch          == null || oldVal == InstrParam.PITCH)            params.add(InstrParam.PITCH);
		if (relVolume      == null || oldVal == InstrParam.RELVOLUME)        params.add(InstrParam.RELVOLUME);
		if (pan			   == null || oldVal == InstrParam.PAN)				 params.add(InstrParam.PAN);
		if (delayEcho      == null || oldVal == InstrParam.DELAY_ECHO)       params.add(InstrParam.DELAY_ECHO);
		if (feedbackEcho   == null || oldVal == InstrParam.FEEDBACK_ECHO)    params.add(InstrParam.FEEDBACK_ECHO);
		if (delayReverb    == null || oldVal == InstrParam.DELAY_REVERB)     params.add(InstrParam.DELAY_REVERB);
		if (feedbackReverb == null || oldVal == InstrParam.FEEDBACK_REVERB)  params.add(InstrParam.FEEDBACK_REVERB);
		if (cutoff         == null || oldVal == InstrParam.CUTOFF)           params.add(InstrParam.CUTOFF);
		InstrParam[] out = new InstrParam[params.size()];
		return params.toArray(out);
	}

	public InstrParam[] getEmptyRangeParams(InstrParam oldVal) {
		List<InstrParam> params = new ArrayList<>();
		if (absVolume      == null || oldVal == InstrParam.ABSVOLUME)        params.add(InstrParam.ABSVOLUME);
		if (onOffEcho      == null || oldVal == InstrParam.ON_OFF_ECHO)      params.add(InstrParam.ON_OFF_ECHO);
		if (onOffReverb    == null || oldVal == InstrParam.ON_OFF_REVERB)    params.add(InstrParam.ON_OFF_REVERB);
		if (onOffFilter    == null || oldVal == InstrParam.ON_OFF_FILTER)    params.add(InstrParam.ON_OFF_FILTER);
		InstrParam[] out = new InstrParam[params.size()];
		return params.toArray(out);
	}

	public boolean isEmpty() {
		for (Field f : getClass().getFields()) {
			if (ExchangeData.class.equals(f.getType())) {
				try {
					if (f.get(this) != null)
						return false;
				} catch (Exception e) {
					// Do nothing
				}
			} else {
				// Do nothing
			}
		}
		return true;
	}

	public Set<SonifiableID> getMappedSonifiables() {
		Set<SonifiableID> set = new HashSet<>(10);
		for (Field f : getClass().getFields()) {
			if (ExchangeData.class.equals(f.getType())) {
				try {
					set.add(((ExchangeData<?>) f.get(this)).getId());
				} catch (Exception e) {
					// Do nothing
				}
			} else {
				// Do nothing
			}
		}
		return set;
	}

	public void rm(InstrParam param) {
		switch (param) {
			case PITCH           -> pitch = null;
			case RELVOLUME       -> relVolume = null;
			case ABSVOLUME       -> absVolume = null;
			case DELAY_ECHO      -> delayEcho = null;
			case FEEDBACK_ECHO   -> feedbackEcho = null;
			case ON_OFF_ECHO     -> onOffEcho = null;
			case DELAY_REVERB    -> delayEcho = null;
			case FEEDBACK_REVERB -> feedbackEcho = null;
			case ON_OFF_REVERB   -> onOffReverb = null;
			case CUTOFF          -> cutoff = null;
			case ON_OFF_FILTER   -> onOffFilter = null;
			case PAN             -> pan = null;
			default              -> {}
		}
	}

	public ExchangeData<? extends ExchangeParam> get(InstrParam param) {
		return switch (param) {
			case PITCH           -> getPitch();
			case RELVOLUME       -> getRelVolume();
			case ABSVOLUME       -> getAbsVolume();
			case DELAY_ECHO      -> getDelayEcho();
			case FEEDBACK_ECHO   -> getFeedbackEcho();
			case ON_OFF_ECHO     -> getOnOffEcho();
			case DELAY_REVERB    -> getDelayEcho();
			case FEEDBACK_REVERB -> getFeedbackEcho();
			case ON_OFF_REVERB   -> getOnOffReverb();
			case CUTOFF          -> getCutoff();
			case ON_OFF_FILTER   -> getOnOffFilter();
			case PAN             -> getPan();
			default              -> null;
		};
	}

	public InstrParam get(ExchangeData<? extends ExchangeParam> ed) {
		if (ed.equals(pitch))          return InstrParam.PITCH;
		if (ed.equals(relVolume))      return InstrParam.RELVOLUME;
		if (ed.equals(pan))      	   return InstrParam.PAN;
		if (ed.equals(delayEcho))      return InstrParam.DELAY_ECHO;
		if (ed.equals(feedbackEcho))   return InstrParam.FEEDBACK_ECHO;
		if (ed.equals(delayReverb))    return InstrParam.DELAY_REVERB;
		if (ed.equals(feedbackReverb)) return InstrParam.FEEDBACK_REVERB;
		if (ed.equals(cutoff))         return InstrParam.CUTOFF;
		if (ed.equals(absVolume))      return InstrParam.ABSVOLUME;
		if (ed.equals(onOffEcho))      return InstrParam.ON_OFF_ECHO;
		if (ed.equals(onOffReverb))    return InstrParam.ON_OFF_REVERB;
		if (ed.equals(onOffFilter))    return InstrParam.ON_OFF_FILTER;
		return null;
	}

	public InstrumentEnum getInstrument() {
		return this.instrument;
	}

	public ExchangeData<LineData> getRelVolume() {
		return this.relVolume;
	}

	public void setRelVolume(ExchangeData<LineData> relVolume) {
		this.relVolume = relVolume;
	}

	public ExchangeData<RangeData> getAbsVolume() {
		return this.absVolume;
	}

	public void setAbsVolume(ExchangeData<RangeData> absVolume) {
		this.absVolume = absVolume;
	}

	public ExchangeData<LineData> getPitch() {
		return this.pitch;
	}

	public void setPitch(ExchangeData<LineData> pitch) {
		this.pitch = pitch;
	}

	public ExchangeData<LineData> getDelayEcho() {
		return this.delayEcho;
	}

	public void setDelayEcho(ExchangeData<LineData> delayEcho) {
		this.delayEcho = delayEcho;
	}

	public ExchangeData<LineData> getFeedbackEcho() {
		return this.feedbackEcho;
	}

	public void setFeedbackEcho(ExchangeData<LineData> feedbackEcho) {
		this.feedbackEcho = feedbackEcho;
	}

	public ExchangeData<RangeData> getOnOffEcho() {
		return this.onOffEcho;
	}

	public void setOnOffEcho(ExchangeData<RangeData> onOffEcho) {
		this.onOffEcho = onOffEcho;
	}

	public ExchangeData<LineData> getDelayReverb() {
		return this.delayReverb;
	}

	public void setDelayReverb(ExchangeData<LineData> delayReverb) {
		this.delayReverb = delayReverb;
	}

	public ExchangeData<LineData> getFeedbackReverb() {
		return this.feedbackReverb;
	}

	public void setFeedbackReverb(ExchangeData<LineData> feedbackReverb) {
		this.feedbackReverb = feedbackReverb;
	}

	public ExchangeData<RangeData> getOnOffReverb() {
		return this.onOffReverb;
	}

	public void setOnOffReverb(ExchangeData<RangeData> onOffReverb) {
		this.onOffReverb = onOffReverb;
	}

	public ExchangeData<LineData> getCutoff() {
		return this.cutoff;
	}

	public void setCutoff(ExchangeData<LineData> cutoff) {
		this.cutoff = cutoff;
	}

	public ExchangeData<RangeData> getOnOffFilter() {
		return this.onOffFilter;
	}

	public void setOnOffFilter(ExchangeData<RangeData> onOffFilter) {
		this.onOffFilter = onOffFilter;
	}

	public boolean getHighPass() {
		return this.highPass;
	}

	public void setHighPass(boolean highPass) {
		this.highPass = highPass;
	}

	public ExchangeData<LineData> getPan() {
		return this.pan;
	}

	public void setPan(ExchangeData<LineData> pan) {
		this.pan = pan;
	}

	@Override
	public String toString() {
		return "{" +
				" instrument='" + this.instrument + "'" +
				", absVolume='" + this.absVolume + "'" +
				", relVolume='" + this.relVolume + "'" +
				", pitch='" + this.pitch + "'" +
				", pan='" + this.pan + "'" +
				", delayEcho='" + this.delayEcho + "'" +
				", feedbackEcho='" + this.feedbackEcho + "'" +
				", onOffEcho='" + this.onOffEcho + "'" +
				", delayReverb='" + this.delayReverb + "'" +
				", feedbackReverb='" + this.feedbackReverb + "'" +
				", onOffReverb='" + this.onOffReverb + "'" +
				", cutoff='" + this.cutoff + "'" +
				", onOffFilter='" + this.onOffFilter + "'" +
				", highPass='" + this.highPass + "'" +
				"}";
	}
}
