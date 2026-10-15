/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class CaseContinuation {

    float pick(TaskId task, Source sampleSource, Task shapeTask, Task patternTask)
    {
        switch(task) {

            case SHAPE: /* FALL-THROUGH */
            case PATTERN: {
                    (task == TaskId.SHAPE ? shapeTask : patternTask)
                            .train(
                                List.of( new EmbeddingTask.Sample( sampleSource.image(), task.conceptId() ) )
                            );
                    float minFloor = task == TaskId.SHAPE && sampleSource.captureIsPicked() && sampleSource.isClean()
                            ? shapeTask.pickerShapeFloor() : 0f;
                    return minFloor;
                }

            default:
                return 0f;

        } // switch
    }

} // class CaseContinuation
