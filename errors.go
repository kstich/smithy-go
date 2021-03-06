package smithy

import "fmt"

// APIError provides the generic API and protocol agnostic error type all SDK
// generated exception types will implement.
type APIError interface {
	error

	// ErrorCode returns the error code for the API exception.
	ErrorCode() string
	// ErrorMessage returns the error message for the API exception.
	ErrorMessage() string
	// ErrorFault returns the fault for the API exception.
	ErrorFault() ErrorFault
}

// GenericAPIError provides a generic concrete API error type that SDKs can use
// to deserialize error responses into. Should be used for unmodeled or untyped
// errors.
type GenericAPIError struct {
	Code    string
	Message string
	Fault   ErrorFault
}

// ErrorCode returns the error code for the API exception.
func (e *GenericAPIError) ErrorCode() string { return e.Code }

// ErrorMessage returns the error message for the API exception.
func (e *GenericAPIError) ErrorMessage() string { return e.Message }

// ErrorFault returns the fault for the API exception.
func (e *GenericAPIError) ErrorFault() ErrorFault { return e.Fault }

func (e *GenericAPIError) Error() string {
	return fmt.Sprintf("api error %s: %s", e.Code, e.Message)
}

var _ APIError = (*GenericAPIError)(nil)

// OperationError decorates an underlying error which occurred while invoking
// an operation with names of the operation and API.
type OperationError struct {
	ServiceName   string
	OperationName string
	Err           error
}

// Service returns the name of the API service the error occurred with.
func (e *OperationError) Service() string { return e.ServiceName }

// Operation returns the name of the API operation the error occurred with.
func (e *OperationError) Operation() string { return e.OperationName }

// Unwrap returns the nested error if any, or nil.
func (e *OperationError) Unwrap() error { return e.Err }

func (e *OperationError) Error() string {
	return fmt.Sprintf("operation error %s: %s, %v", e.ServiceName, e.OperationName, e.Err)
}

// ErrorFault provides the type for a Smithy API error fault.
type ErrorFault int

// ErrorFault enumeration values
const (
	FaultUnknown ErrorFault = iota
	FaultServer
	FaultClient
)

func (f ErrorFault) String() string {
	switch f {
	case FaultServer:
		return "server"
	case FaultClient:
		return "client"
	default:
		return "unknown"
	}
}
