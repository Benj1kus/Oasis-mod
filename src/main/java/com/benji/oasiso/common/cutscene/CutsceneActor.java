package com.benji.oasiso.common.cutscene;

public interface CutsceneActor {
    void setCutscene(String animation, long startTick);

    void endCutscene();
}
