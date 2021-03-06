$version: "1"

metadata suppressions = [{
    id: "UnstableFeature",
    namespace: "example.weather"
}]

namespace example.weather

use aws.protocols#awsJson1_1

/// Provides weather forecasts.
@awsJson1_1
@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "pageSize")
service Weather {
    version: "2006-03-01",
    resources: [City],
    operations: [GetCurrentTime]
}

resource City {
    identifiers: { cityId: CityId },
    read: GetCity,
    list: ListCities,
    resources: [Forecast, CityImage],
}

resource Forecast {
    identifiers: { cityId: CityId },
    read: GetForecast,
}

resource CityImage {
    identifiers: { cityId: CityId },
    read: GetCityImage,
}

// "pattern" is a trait.
@pattern("^[A-Za-z0-9 ]+$")
string CityId

@readonly
@http(method: "GET", uri: "/cities/{cityId}")
operation GetCity {
    input: GetCityInput,
    output: GetCityOutput,
    errors: [NoSuchResource]
}

/// The input used to get a city.
structure GetCityInput {
    // "cityId" provides the identifier for the resource and
    // has to be marked as required.
    @required
    @httpLabel
    cityId: CityId
}

structure GetCityOutput {
    // "required" is used on output to indicate if the service
    // will always provide a value for the member.
    @required
    name: String,

    @required
    coordinates: CityCoordinates,

    city: CitySummary,

    metadata: CityMetadata
}

document CityMetadata

// This structure is nested within GetCityOutput.
structure CityCoordinates {
    @required
    latitude: PrimitiveFloat,

    @required
    longitude: Float,
}

/// Error encountered when no resource could be found.
@error("client")
@httpError(404)
structure NoSuchResource {
    /// The type of resource that was not found.
    @required
    resourceType: String,

    message: String,
}

// The paginated trait indicates that the operation may
// return truncated results.
@readonly
@paginated(items: "items")
@http(method: "GET", uri: "/cities")
operation ListCities {
    input: ListCitiesInput,
    output: ListCitiesOutput
}

structure ListCitiesInput {
    @httpQuery("nextToken")
    nextToken: String,

    @httpQuery("pageSize")
    pageSize: Integer
}

structure ListCitiesOutput {
    nextToken: String,

    @required
    items: CitySummaries,
}

// CitySummaries is a list of CitySummary structures.
list CitySummaries {
    member: CitySummary
}

// CitySummary contains a reference to a City.
@references([{resource: City}])
structure CitySummary {
    @required
    cityId: CityId,

    @required
    name: String,

    number: String,
    case: String,
}

@readonly
@http(method: "GET", uri: "/current-time")
operation GetCurrentTime {
    output: GetCurrentTimeOutput
}

structure GetCurrentTimeOutput {
    @required
    time: Timestamp
}

@readonly
@http(method: "GET", uri: "/cities/{cityId}/forecast")
operation GetForecast {
    input: GetForecastInput,
    output: GetForecastOutput
}

// "cityId" provides the only identifier for the resource since
// a Forecast doesn't have its own.
structure GetForecastInput {
    @required
    @httpLabel
    cityId: CityId,
}

structure GetForecastOutput {
    chanceOfRain: Float,
    precipitation: Precipitation,
}

/// Different kinds of preciptation.
union Precipitation {
    rain: PrimitiveBoolean,
    sleet: PrimitiveBoolean,
    hail: StringMap,
    snow: SimpleYesNo,
    mixed: TypedYesNo,
    other: OtherStructure,
    blob: Blob,
    baz: example.weather.nested.more#Baz,
}

structure OtherStructure {}

@enum([{value: "YES"}, {value: "NO"}])
string SimpleYesNo

/// yes or no
@enum([
    {
        value: "Yes",
        name: "YES",
        documentation: "yes",
    },
    {
        value: "No",
        name: "NO",
        documentation: "no",
    }
])
string TypedYesNo

map StringMap {
    key: String,
    value: String,
}

@readonly
@http(method: "GET", uri: "/cities/{cityId}/image")
operation GetCityImage {
    input: GetCityImageInput,
    output: GetCityImageOutput,
    errors: [NoSuchResource]
}

structure GetCityImageInput {
    @required @httpLabel
    cityId: CityId,
}

structure GetCityImageOutput {
    /// An image of the city in JPEG format.
    @httpPayload
    image: CityImageData,
}

/// A JPEG image of a city.
@streaming
@mediaType("image/jpeg")
blob CityImageData
