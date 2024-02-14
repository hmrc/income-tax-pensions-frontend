package utils

import models.pension.charges.{CreateUpdatePensionChargesRequestModel, PensionContributions}

object ModelHelpers {

  // Pension charges
  val emptyPensionContributions: PensionContributions =
    PensionContributions(Seq.empty, 0.00, 0.00, None, None, None)

  val emptyChargesDownstreamRequestModel: CreateUpdatePensionChargesRequestModel =
    CreateUpdatePensionChargesRequestModel(None, None, None, None, None)
}
