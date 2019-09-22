package models

import java.sql.Connection

import anorm._
import components.util.Clock
import javax.inject.Inject

class AccountAccessDao @Inject() (
  clock: Clock
) extends AbstractDao[AccountAccess, String] {

  val tableName = "account_access"

  val idColumn = "code"

  val columns = Seq(
    "code", "account_id",
    "user_agent", "remote_address", "created_at"
  )

  val rowParser: RowParser[AccountAccess] = {
    import anorm.SqlParser._
    get[String]("code") ~
      get[Long]("account_id") ~
      get[String]("user_agent") ~
      get[String]("remote_address") ~
      get[Option[java.util.Date]]("created_at") map {
      case code ~ accountId ~ userAgent ~ remoteAddress ~ createdAt =>
        AccountAccess(code, accountId, userAgent, remoteAddress,
          createdAt.map { ts => new java.util.Date(ts.getTime) })
    }
  }

  override def toNamedParameter(accountAccess: AccountAccess) = {
    Seq[NamedParameter](
      'code -> accountAccess.code,
      'account_id -> accountAccess.accountId,
      'user_agent -> accountAccess.userAgent,
      'remote_address -> accountAccess.remoteAddress,
      'created_at -> accountAccess.createdAt
    )
  }

  override def onUpdate(accountAccess: AccountAccess): AccountAccess = {
    val t = clock.currentTimeMillis
    accountAccess.copy(
      createdAt = accountAccess.createdAt.orElse(Some(new java.util.Date(t)))
    )
  }

  private val selectRemovableCodesQuery = SQL(
    """
      |SELECT code FROM account_access
      | WHERE account_id = {accountId}
      | ORDER BY created_at DESC
    """.stripMargin
  )

  private val deleteByCodesQuery = SQL(
    """
      |DELETE FROM account_access
      | WHERE code IN ({removableCodes})
    """.stripMargin
  )

  def add(entity: AccountAccess, maxRows: Int = 10)(implicit conn: Connection): AccountAccess = {
    require(maxRows > 0)
    // Clean up exceeded rows
    val removableCode = selectRemovableCodesQuery
      .on('accountId -> entity.accountId)
      .as(SqlParser.str("code").*)(conn)
      .drop(maxRows - 1)
    if (!removableCode.isEmpty)
      deleteByCodesQuery.on('removableCodes -> removableCode).execute()(conn)
    // and then insert it
    create(entity)(conn)
  }

  private val selectByAccountIdQuery = SQL(
    s"""
       |SELECT * FROM account_access
       | WHERE account_id = {accountId}
       | ORDER BY created_at DESC
    """.stripMargin
  )

  def selectByAccountId(accountId: Long)(implicit conn: Connection): Seq[AccountAccess] =
    selectByAccountIdQuery.on('accountId -> accountId).as(rowParser.*)(conn)
}
