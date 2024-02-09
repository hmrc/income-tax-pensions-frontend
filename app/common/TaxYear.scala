package common

/** Represents a tax year in the format yyyy - yyyy+1, e.g. 2023-2024. We keep only the last part (2024) which is the end of the tax year.
  */
final case class TaxYear(endYear: Int) extends AnyVal {
  override def toString: String = endYear.toString
}
