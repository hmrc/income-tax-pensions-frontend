# income-tax-pensions-frontend

This is where users can review and make changes to the Pensions section of their income tax return.

## Running the service locally

you will need to have the following:
- Installed [MongoDB](https://docs.mongodb.com/manual/installation/)
- Installed/configured [service-manager](https://github.com/hmrc/sm2)

The service manager profile for this service is:

### MongoDB 
```shell
   sudo mongod
```

### Service
```shell
   sm2 --start INCOME_TAX_PENSIONS_FRONTEND
```
   
Run the following command to start the remaining services locally:

```shell
    sm2 --start INCOME_TAX_SUBMISSION_ALL
```

To run the service locally:

```shell
    sm2 --start INCOME_TAX_SUBMISSION_ALL
    sm2 --stop INCOME_TAX_PENSIONS_FRONTEND     
    sbt -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes run
```

To test coverage locally

```shell
   sbt clean compile coverage test it/test coverageReport
```

This service runs on port: `localhost:9321`

### Feature Switches

| Feature | Environments Enabled In |
| --- |-----------------------|
| Encryption | QA, Staging, Production |
| Welsh Toggle | QA, Staging           |
| taxYearError | Production            |

## Pensions Journeys

- Payments into pensions
- Income from pensions
- Annual and lifetime allowances
- Unauthorised payments from pensions
- Overseas pensions

### Backends

This service pulls pensions data from [income-tax-pensions](https://github.com/hmrc/income-tax-pensions) and [income-tax-submission](https://github.com/hmrc/income-tax-submission)

### Employment Sources (HMRC-Held and Customer Data) - for Income From Pensions
Data for Income from pensions comes  from employment data through income-tax-submission. Employment data can come from different sources: HMRC-Held and Customer. 

HMRC-Held data is employment data that HMRC has for the user within the tax year, prior to any updates made by the user. The Income from pensions data displayed in-year is HMRC-Held.

Customer data is provided by the user. At the end of the tax year, users can view any existing employment data and make changes (create, update and delete).


## Ninos with stub data for Pensions

### In-Year
| Nino      | Pensions data                                                 |
|-----------|---------------------------------------------------------------|
| AA370343B | User with pension reliefs, pension charges and state benefits |
| AA123459A | User with pension reliefs, pension charges and state benefits |

### End of Year
| Nino      | Pensions data                                                 |
|-----------|---------------------------------------------------------------|
| AA370343B | User with pension reliefs, pension charges and state benefits |
| AA123459A | User with pension reliefs, pension charges and state benefits |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
