/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
sealed class TestStepBuilder<InputArtifact, OutputArtifact, out FacadeStep>
    where InputArtifact : ResultingArtifact<InputArtifact>,
          OutputArtifact : ResultingArtifact<OutputArtifact>,
          FacadeStep : TestStep<InputArtifact, OutputArtifact> {

    sealed class FacadeStepBuilder<InputArtifact, OutputArtifact, Facade, FacadeStep>(
        val facade: Constructor<Facade>,
    ) : TestStepBuilder<InputArtifact, OutputArtifact, FacadeStep>()
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact>,
              Facade : AbstractTestFacadeBase<InputArtifact, OutputArtifact>,
              FacadeStep : TestStep<InputArtifact, OutputArtifact> {

        class NonGroupingStage<InputArtifact, OutputArtifact>(
            facade: Constructor<AbstractTestFacade<InputArtifact, OutputArtifact>>,
        ) : FacadeStepBuilder<
                InputArtifact,
                OutputArtifact,
                AbstractTestFacade<InputArtifact, OutputArtifact>,
                TestStep.NonGroupingStep.FacadeStep<InputArtifact, OutputArtifact>
                >(facade) where InputArtifact : ResultingArtifact<InputArtifact>,
                                OutputArtifact : ResultingArtifact<OutputArtifact> {
            fun x() {}
        }
    }
}
