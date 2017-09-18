package eu.innovation.engineering.LSA.keywordExtractor;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.MatrixDimensionMismatchException;
import org.apache.commons.math3.linear.NonSquareMatrixException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixChangingVisitor;
import org.apache.commons.math3.linear.RealMatrixPreservingVisitor;
import org.apache.commons.math3.linear.RealVector;

public class SVDMatrix implements RealMatrix {

  @Override
  public int getColumnDimension() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getRowDimension() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public boolean isSquare() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public RealMatrix add(RealMatrix arg0) throws MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addToEntry(int arg0, int arg1, double arg2) throws OutOfRangeException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public RealMatrix copy() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void copySubMatrix(int[] arg0, int[] arg1, double[][] arg2) throws OutOfRangeException, NullArgumentException, NoDataException, MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void copySubMatrix(int arg0, int arg1, int arg2, int arg3, double[][] arg4) throws OutOfRangeException, NumberIsTooSmallException, MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public RealMatrix createMatrix(int arg0, int arg1) throws NotStrictlyPositiveException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double[] getColumn(int arg0) throws OutOfRangeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix getColumnMatrix(int arg0) throws OutOfRangeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealVector getColumnVector(int arg0) throws OutOfRangeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double[][] getData() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getEntry(int arg0, int arg1) throws OutOfRangeException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getFrobeniusNorm() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getNorm() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double[] getRow(int arg0) throws OutOfRangeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix getRowMatrix(int arg0) throws OutOfRangeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealVector getRowVector(int arg0) throws OutOfRangeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix getSubMatrix(int[] arg0, int[] arg1) throws NullArgumentException, NoDataException, OutOfRangeException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix getSubMatrix(int arg0, int arg1, int arg2, int arg3) throws OutOfRangeException, NumberIsTooSmallException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getTrace() throws NonSquareMatrixException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public RealMatrix multiply(RealMatrix arg0) throws DimensionMismatchException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void multiplyEntry(int arg0, int arg1, double arg2) throws OutOfRangeException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public double[] operate(double[] arg0) throws DimensionMismatchException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealVector operate(RealVector arg0) throws DimensionMismatchException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix power(int arg0) throws NotPositiveException, NonSquareMatrixException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix preMultiply(RealMatrix arg0) throws DimensionMismatchException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double[] preMultiply(double[] arg0) throws DimensionMismatchException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealVector preMultiply(RealVector arg0) throws DimensionMismatchException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix scalarAdd(double arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix scalarMultiply(double arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setColumn(int arg0, double[] arg1) throws OutOfRangeException, MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setColumnMatrix(int arg0, RealMatrix arg1) throws OutOfRangeException, MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setColumnVector(int arg0, RealVector arg1) throws OutOfRangeException, MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setEntry(int arg0, int arg1, double arg2) throws OutOfRangeException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setRow(int arg0, double[] arg1) throws OutOfRangeException, MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setRowMatrix(int arg0, RealMatrix arg1) throws OutOfRangeException, MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setRowVector(int arg0, RealVector arg1) throws OutOfRangeException, MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setSubMatrix(double[][] arg0, int arg1, int arg2) throws NoDataException, OutOfRangeException, DimensionMismatchException, NullArgumentException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public RealMatrix subtract(RealMatrix arg0) throws MatrixDimensionMismatchException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealMatrix transpose() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double walkInColumnOrder(RealMatrixChangingVisitor arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInColumnOrder(RealMatrixPreservingVisitor arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInColumnOrder(RealMatrixChangingVisitor arg0, int arg1, int arg2, int arg3, int arg4) throws OutOfRangeException, NumberIsTooSmallException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInColumnOrder(RealMatrixPreservingVisitor arg0, int arg1, int arg2, int arg3, int arg4) throws OutOfRangeException, NumberIsTooSmallException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInOptimizedOrder(RealMatrixChangingVisitor arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInOptimizedOrder(RealMatrixPreservingVisitor arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInOptimizedOrder(RealMatrixChangingVisitor arg0, int arg1, int arg2, int arg3, int arg4) throws OutOfRangeException, NumberIsTooSmallException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInOptimizedOrder(RealMatrixPreservingVisitor arg0, int arg1, int arg2, int arg3, int arg4) throws OutOfRangeException, NumberIsTooSmallException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInRowOrder(RealMatrixChangingVisitor arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInRowOrder(RealMatrixPreservingVisitor arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInRowOrder(RealMatrixChangingVisitor arg0, int arg1, int arg2, int arg3, int arg4) throws OutOfRangeException, NumberIsTooSmallException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double walkInRowOrder(RealMatrixPreservingVisitor arg0, int arg1, int arg2, int arg3, int arg4) throws OutOfRangeException, NumberIsTooSmallException {
    // TODO Auto-generated method stub
    return 0;
  }

}
